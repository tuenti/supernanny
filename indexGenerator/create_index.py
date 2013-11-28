#!/usr/bin/env python

import os
import sys
import subprocess
import gzip
import re
import tempfile
import shutil
from optparse import OptionParser

EXTENSION_TAR_OPTS = {".tar.bz2": "j",
                      ".tar.gz": "z",
                      ".tar.xz": "J"}
UNKNOWN_MD5 = "unknown"


def splitValidNames(pkg):
    f = os.path.basename(pkg)
    for e in EXTENSION_TAR_OPTS.iterkeys():
        pos = f.find(e)
        if pos != -1:
            name = f[:pos]
            extension = f[pos:]
            return (pkg, name, extension)

    return None


##########
# Index related functions


def readField(field, index):
    line = index.readline()
    if line == "":
        raise EOFError()

    line = line.strip()
    if line.startswith(field):
        return line[len(field):].strip()

    raise Exception("Expected field \"" + field
                    + "\" but got \"" + line + "\"")


def readDeps(index):
    deps = []
    while True:
        line = index.readline()
        if line == "":
            raise EOFError()

        line = line.strip()
        if line == "":
            return deps

        deps.append(line)


def readIndex(filename):
    index = gzip.open(filename, 'rb')
    idx = {}
    while True:
        entry = {}
        try:
            entry["Name"] = readField("Name:", index)
        except EOFError, e:
            break

        entry["Version"] = readField("Version:", index)
        entry["File"] = readField("File:", index)
        entry["MD5"] = readField("MD5:", index)
        readField("Deps:", index)
        entry["Deps"] = readDeps(index)
        idx[entry["File"]] = entry

    return idx


def writeIndex(existingIndex, output):
    for pkg, entry in existingIndex.iteritems():
        for f in ["Name", "Version", "File", "MD5"]:
            output.write(f + ": " + entry[f] + "\n")

        output.write("Deps:\n")
        for d in entry["Deps"]:
            output.write("  " + d + "\n")

        output.write("\n")


##########
# MD5s


def getMd5s(filelist):
    pargs = ["md5sum"]
    pargs.extend(filelist)
    hasher = subprocess.Popen(pargs, stdout=subprocess.PIPE,
                              stderr=subprocess.PIPE)
    (out, err) = hasher.communicate()
    p = re.compile(r'\\s+')
    hashes = {}
    for l in out.splitlines():
        data = l.strip().split()
        hashes[data[1]] = data[0]

    return hashes


def getFakeMd5s(filelist):
    hashes = {}
    for f in filelist:
        hashes[f] = "unknown"

    return hashes


##########
# Index generation


def getDeps(f, ext):
    opts = "xfO"

    opts += EXTENSION_TAR_OPTS[ext]

    cmd = "tar %s %s ./.DEP" % (opts, f)
    tar = subprocess.Popen(["tar", opts, f, "./.DEP"],
                           stdout=subprocess.PIPE,
                           stderr=subprocess.PIPE)
    (out, err) = tar.communicate()
    # exclude comments and empty lines
    deps = []
    for l in out.splitlines():
        l = l.strip()
        if not l.startswith("#") and l != "":
            deps.append(l)

    return deps


def processPkg(pkg, relFilename, name, extension, hashes, get_deps):
    split = name.rsplit("-", 1)
    name = split[0]
    version = split[1]

    #strip any possible suffixes from the version
    under_pos = version.find("_")
    if under_pos != -1:
        version = version[:under_pos]

    deps = []
    if get_deps:
        deps = getDeps(pkg, extension)

    entry = {"Name": name,
             "Version": version,
             "File": relFilename,
             "MD5": hashes.get(pkg, UNKNOWN_MD5),
             "Deps": deps}

    return entry


def makeNewIndex(pkgs, repo, existingIndex, md5s, get_deps):
    newIndex = {}
    for (pkg, name, extension) in pkgs:
        # make relative path
        relFilename = os.path.relpath(pkg, repo)

        # check if it has changed since the index was generated
        changed = True
        if relFilename in existingIndex:
            entry = existingIndex[relFilename]
            if entry["MD5"] == md5s.get(pkg, UNKNOWN_MD5):
                changed = False

        if changed:
            entry = processPkg(pkg, relFilename, name, extension,
                               md5s, options.get_deps)

        newIndex[relFilename] = entry

    return newIndex

if __name__ == "__main__":
    #parse args
    usage = "usage: %prog [options] repository_path [gz_index_filename]"
    parser = OptionParser(usage=usage)
    parser.add_option("-f", "--read-from-file", action="store",
                      dest="read_from_file", default=None,
                      help="Read file list from file instead of repo"
                      " - implies -n, and will generate invalid MD5s"
                      " - use for testing only")
    parser.add_option("-n", "--no-deps", action="store_false", dest="get_deps",
                      default=True, help="don't get deps for artifacts")
    parser.add_option("-m", "--no-md5", action="store_false", dest="get_md5",
                      default=True, help="don't calc md5 for artifacts")
    parser.add_option("-r", "--recursive", action="store_true",
                      dest="recursive", default=False,
                      help="descend into any directories recursively")

    (options, args) = parser.parse_args()

    if len(args) < 1:
        parser.error("missing repository_path argument")

    repo = os.path.abspath(args[0])
    indexFile = None
    if len(args) > 1:
        indexFile = args[1]

    # get all valid packages to process
    if options.read_from_file:
        ls = open(options.read_from_file, "r")
        filelist = [os.path.join(repo, l.strip()) for l in ls.readlines()]
        ls.close()
    else:
        if options.recursive:
            filelist = []
            for root, dir, files in os.walk(repo):
                filelist.extend([os.path.join(root, f) for f in files])
        else:
            filelist = [os.path.join(repo, f) for f in os.listdir(repo)]

    # filter out valid filenames
    pkgs = []
    for f in filelist:
        info = splitValidNames(f)
        if info:
            pkgs.append(info)

    # get all md5s
    if options.get_md5:
        md5s = getMd5s(filelist)
    else:
        md5s = getFakeMd5s(filelist)

    # read existing index
    existingIndex = {}
    if indexFile:
        try:
            existingIndex = readIndex(indexFile)
        except:
            sys.stderr.write("Existing index broken - regenerating...\n")
    else:
        output = sys.stdout

    # create new index
    newIndex = makeNewIndex(pkgs, repo, existingIndex, md5s, options.get_deps)

    # if the index has changed, write it
    if newIndex != existingIndex:
        if indexFile:
            fd, tmpfile = tempfile.mkstemp(dir=os.path.dirname(indexFile),
                                           prefix=".index")
            os.close(fd)
            output = gzip.open(tmpfile, 'wb')

        writeIndex(newIndex, output)
        output.flush()
        output.close()

        if indexFile:
            os.rename(tmpfile, indexFile)
            sys.stderr.write("Updated index\n")
    else:
        sys.stderr.write("Index up to date\n")
