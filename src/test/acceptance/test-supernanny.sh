#!/bin/sh
#
# Acceptance test for Supernanny
#
# Requirements:
# - Mercurial
# - Git
# - Apache with WebDAV module
# - ant
#
# Syntax:
#   test-supernanny.sh <path to the supernanny jar file under test>
#
# Exit codes:
# - 0: All tests passed
# - 1: Some environment error happened (wrong syntax, missing required software...)
# - 2: Some test failed
#
# @author Jesus Bravo Alvarez <suso@tuenti.com>


index_generator=$PWD/../../../indexGenerator/maintain-index.sh

##################
# Common methods

cleanup() {
  echo
  colorTitle "Cleaning up..."
  [ -r $testdir/apache/httpd.pid ] && kill `cat $testdir/apache/httpd.pid`
  [ -r $testdir/hgserve.pid ] && kill `cat $testdir/hgserve.pid`
  kill $index_generator_pid
  rm -rf $testdir
}

fail() {
  colorFail "$1"
  cleanup
  exit 2
}

die() {
  echo "$1"
  cleanup
  exit 1
}

syntax() {
  echo "Syntax: test-supernanny.sh <path/to/supernanny.jar>"
  exit 1
}

colorTitle() {
  /bin/echo -e "\e[1;34m$1\e[0m"
}

colorPass() {
  /bin/echo -e "\e[1;32m$1\e[0m"
}

colorFail() {
  /bin/echo -e "\e[1;31m$1\e[0m"
}


##################
# Initialization

[ "$1" = "" ] && syntax

jar=$1

if [ ! -r "$jar" ]; then
  echo "JAR file $jar not found"
  syntax
fi

testdir=/tmp/test-supernanny-$$
mkdir -p $testdir

# Call die on Control+C to cleanup
trap 'die' 2


##############################
# Configure apache+WebDAV

colorTitle "Configuring and launching apache..."

cd $testdir
mkdir apache
cat > apache/httpd.conf <<EOF
PidFile "httpd.pid"
LoadModule dav_module modules/mod_dav.so
LoadModule dav_fs_module modules/mod_dav_fs.so
ErrorLog "/dev/null"
Listen 53417
ServerName localhost
ServerLimit 2
DocumentRoot "artifacts"
DavLockDB "webdav.db"
<Location />
  Dav on
</Location>
EOF

# Detect Apache2 installation parameters
if [ -r /usr/lib/apache2/modules/mod_dav.so ]; then
  ln -s /usr/lib/apache2/modules apache/modules
  apachecmd=/usr/sbin/apache2
elif [ -r /usr/lib/httpd/modules/mod_dav.so ]; then
  ln -s /usr/lib/httpd/modules apache/modules
  apachecmd=/usr/sbin/httpd
else
  die "Required Apache and mod_dav.so not found"
fi

artifacts=$testdir/apache/artifacts
mkdir $artifacts

$index_generator $artifacts &
index_generator_pid=$!

touch $artifacts/supernanny-test-1.0.tar.bz2

$apachecmd -d $testdir/apache -f httpd.conf
[ $? -eq 0 ] || die "Apache failed to execute. Another instance already running?"


#####################################################
# Setup paths for publishing and fetching artifacts

echo
colorTitle "Setting up publish/fetch paths..."

mkdir -p $testdir/publisher/build
cd $testdir/publisher
echo "1.0" > build/version.txt
cat > .EXPORT <<EOF
supernanny-test TARBZ2 http://localhost:53417/ build/
EOF
cat > build.xml <<EOF
<?xml version="1.0"?>
<project name="test-supernanny">
    <taskdef name="supernanny-publish" classname="com.tuenti.supernanny.ant.SuperNannyPublish" classpath="$jar"/>
    <target name="publish-major"><supernanny-publish next="+"/></target>
    <target name="publish-minor"><supernanny-publish next="x.+"/></target>
</project>
EOF

mkdir -p $testdir/publisher_xz/build
cd $testdir/publisher_xz
echo "1.0" > build/version.txt
cat > .EXPORT <<EOF
supernanny-test TARXZ http://localhost:53417/ build/
EOF

mkdir -p $testdir/fetcher
cd $testdir/fetcher
cat > build.xml <<EOF
<?xml version="1.0"?>
<project name="test-supernanny">
    <taskdef name="supernanny-fetch" classname="com.tuenti.supernanny.ant.SuperNannyResolve" classpath="$jar"/>
    <target name="get-deps"><supernanny-fetch depFile="lib/DEP.override,.DEP"/></target>
    <target name="get-deps-partial"><supernanny-fetch depFile="lib/DEP.override,.DEP" skipCleanup="true"/></target>
</project>
EOF


#######################
# Setup HG repository

echo
colorTitle "Creating and configuring HG repository..."

# Create master repository and launch hg http server (we need http
# because some supernanny processes depend on it)
mkdir $testdir/remote.hg
cd $testdir/remote.hg
hg init
cat >> .hg/hgrc <<EOF
[web]
allow_push = *
push_ssl = false
EOF
hg serve --port 53418 --pid $testdir/hgserve.pid --daemon
[ $? -eq 0 ] || die "Hg serve failed to execute. Another instance already running?"

# Clone the master repo and do the initial setup
cd $testdir
hg clone http://localhost:53418/ repo.hg
cd repo.hg
mkdir -p libraries/supernanny-test
echo "1.0" > libraries/supernanny-test/version.txt
cat > libraries/supernanny-test/.EXPORT <<EOF
supernanny-test MERCURIAL http://localhost:53418/ .
EOF
hg add libraries/supernanny-test/
hg commit -m "Version 1.0"
hg tag supernanny-test-1.0
hg push



########################
# Setup GIT repository

echo
colorTitle "Creating and configuring GIT repository..."

# Create master repository
mkdir $testdir/remote.git
cd $testdir/remote.git
git init --bare

# Clone the master repo and do the initial setup
cd $testdir
git clone remote.git repo.git
cd repo.git
git checkout -b master
mkdir -p libraries/supernanny-test
echo "1.0" > libraries/supernanny-test/version.txt
cat > libraries/supernanny-test/.EXPORT <<EOF
supernanny-test GIT file://$testdir/remote.git/ .
EOF
git add libraries/supernanny-test/version.txt
git commit -m "Version 1.0"
git tag supernanny-test-1.0
git push origin refs/*
cd ..


##########
# Test 1

echo
colorTitle "Test 1: Publish minor version"

cd $testdir/publisher
echo "1.1" > build/version.txt
java -jar $jar publish -f --next x.+ < /dev/null

[ -r $artifacts/supernanny-test-1.1.tar.bz2 ] ||
fail "Test 1: Artifact supernanny-test-1.1.tar.bz2 not created"

colorPass "Test 1 passed OK"


##########
# Test 2

echo
colorTitle "Test 2: Publish minor version with suffix"

cd $testdir/publisher
echo "1.2" > build/version.txt
java -jar $jar publish -f --next x.+ --suffix 1a2b3c4d < /dev/null

[ -r $artifacts/supernanny-test-1.2_1a2b3c4d.tar.bz2 ] ||
fail "Test 2: Artifact supernanny-test-1.2_1a2b3c4d.tar.bz2 not created"

colorPass "Test 2 passed OK"


##########
# Test 3

echo
colorTitle "Test 3: Publish major version"

cd $testdir/publisher
echo "2.0" > build/version.txt
java -jar $jar publish -f --next + < /dev/null

[ -r $artifacts/supernanny-test-2.0.tar.bz2 ] ||
fail "Test 3: Artifact supernanny-test-2.0.tar.bz2 not created"

colorPass "Test 3 passed OK"


##########
# Test 4

echo
colorTitle "Test 4: Publish minor version (HG)"

cd $testdir/repo.hg/libraries/supernanny-test
echo "1.1" > version.txt
hg commit -m "Version 1.1"
hg push
java -jar $jar publish -f --next x.+

curl -s http://localhost:53418/tags?style=raw | grep supernanny-test-1.1 > /dev/null
[ $? -eq 0 ] || fail "Test 4: Mercurial tag supernanny-test-1.1 not created"

colorPass "Test 4 passed OK"


##########
# Test 5

echo
colorTitle "Test 5: Publish major version (HG)"

cd $testdir/repo.hg/libraries/supernanny-test
echo "2.0" > version.txt
hg commit -m "Version 2.0"
hg push
java -jar $jar publish -f --next +

curl -s http://localhost:53418/tags?style=raw | grep supernanny-test-2.0 > /dev/null
[ $? -eq 0 ] || fail "Test 5: Mercurial tag supernanny-test-2.0 not created"

colorPass "Test 5 passed OK"


##########
# Test 6

echo
colorTitle "Test 6: Publish minor version (GIT)"

cd $testdir/repo.git/libraries/supernanny-test
echo "1.1" > version.txt
git add version.txt
git commit -m "Version 1.1"
git push
java -jar $jar publish -f --next x.+

git ls-remote --tags | grep supernanny-test-1.1 > /dev/null
[ $? -eq 0 ] || fail "Test 6: Git tag supernanny-test-1.1 not created"

colorPass "Test 4 passed OK"


##########
# Test 7

echo
colorTitle "Test 7: Publish major version (GIT)"

cd $testdir/repo.git/libraries/supernanny-test
echo "2.0" > version.txt
git add version.txt
git commit -m "Version 2.0"
git push
java -jar $jar publish -f --next +

git ls-remote --tags | grep supernanny-test-2.0 > /dev/null
[ $? -eq 0 ] || fail "Test 7: Git tag supernanny-test-2.0 not created"

colorPass "Test 7 passed OK"



###############
# Fetch tests

# Common method for all fetching tests
fetchtest() {
  testnum=$1
  testname=$2
  testtype=$3
  testver=$4
  expected=$5

  if [ "$testtype" = "TARBZ2" ]; then
    url="http://localhost:53417/"
  elif [ "$testtype" = "MERCURIAL" ]; then
    url="http://localhost:53418/"
  elif [ "$testtype" = "GIT" ]; then
    url="file://$testdir/remote.git/"
  fi

  echo
  colorTitle "Test $testnum: $testname"
  cd $testdir/fetcher
  rm -rf lib
  cat > .DEP <<EOF
supernanny-test $testtype $url $testver
EOF
  java -jar $jar fetch

  if [ -r lib/supernanny-test/version.txt ]; then
    repoversion=`cat lib/supernanny-test/version.txt`
  elif [ -r lib/supernanny-test/libraries/supernanny-test/version.txt ]; then
    # Note: this path would normally mean it's a multi-artifact repo,
    # but that's a different test case not covered here
    repoversion=`cat lib/supernanny-test/libraries/supernanny-test/version.txt`
  fi
  [ "$repoversion" = "$expected" ] ||
  fail "Test $testnum: Fetched dependency failed or is not version $expected"

  colorPass "Test $testnum passed OK"
}


##########
# Test 8

fetchtest 8 "Fetch explicit artifact dependency" TARBZ2 "1.1" "1.1"

##########
# Test 9

fetchtest 9 "Fetch explicit artifact dependency (with suffix)" TARBZ2 "1.2" "1.2"

###########
# Test 10

fetchtest 10 "Fetch minor wildcard artifact dependency" TARBZ2 "2.*" "2.0"

###########
# Test 11

fetchtest 11 "Fetch minor wildcard artifact dependency (with suffix)" TARBZ2 "1.*" "1.2"

###########
# Test 12

fetchtest 12 "Fetch major wildcard artifact dependency" TARBZ2 "*" "2.0"

###########
# Test 13

fetchtest 13 "Fetch minor wildcard GIT dependency" GIT "1.*" "1.1"

###########
# Test 14

fetchtest 14 "Fetch major wildcard GIT dependency" GIT "*" "2.0"

###########
# Test 15

fetchtest 15 "Fetch minor wildcard HG dependency" MERCURIAL "1.*" "1.1"

###########
# Test 16

fetchtest 16 "Fetch major wildcard HG dependency" MERCURIAL "*" "2.0"


###########
# Test 17

echo
colorTitle "Preparing test GIT branch..."
cd $testdir/repo.git
git checkout -b test-branch
echo "test-branch" > libraries/supernanny-test/version.txt
git add libraries/supernanny-test/version.txt
git commit -m "Version test-branch"
git push origin test-branch

fetchtest 17 "Fetch branch GIT dependency" GIT "test-branch" "test-branch"


###########
# Test 18

echo
colorTitle "Preparing test HG branch..."
cd $testdir/repo.hg
hg branch test-branch
echo "test-branch" > libraries/supernanny-test/version.txt
hg commit -m "Version test-branch"
hg push --new-branch

fetchtest 18 "Fetch branch HG dependency" MERCURIAL "test-branch" "test-branch"


###########
# Test 19

echo
colorTitle "Test 19: Multiple artifact repository (GIT)"

cd $testdir/fetcher
rm -rf lib
cat > .DEP <<EOF
supernanny-test GIT file://$testdir/remote.git/#libraries/supernanny-test test-branch
EOF
java -jar $jar fetch

[ `cat lib/supernanny-test/version.txt` = "test-branch" ] ||
fail "Test 19: Fetched GIT dependency failed or incorrect version test-branch"

colorPass "Test 19 passed OK"


###########
# Test 20

echo
colorTitle "Test 20: Multiple artifact repository (HG)"

cd $testdir/fetcher
rm -rf lib
cat > .DEP <<EOF
supernanny-test MERCURIAL http://localhost:53418/#libraries/supernanny-test test-branch
EOF
java -jar $jar fetch

[ `cat lib/supernanny-test/version.txt` = "test-branch" ] ||
fail "Test 20: Fetched HG dependency failed or incorrect version test-branch"

colorPass "Test 20 passed OK"


###########
# Test 21

echo
colorTitle "Test 21: Multiple artifact repository (checking it doesn't affect TARBZ2)"

#cd $testdir/fetcher
#rm -rf lib
#cat > .DEP <<EOF
#supernanny-test_supernanny-test TARBZ2 http://localhost:53417/ *
#EOF
#java -jar $jar fetch
#
#[ `cat lib/supernanny-test/version.txt` = "2.0" ] ||
#fail "Test 21: Fetched TARBZ2 dependency failed or incorrect version 2.0"
#
#colorPass "Test 21 passed OK"

colorPass "Test 21 skipped"


###########
# Test 22

echo
colorTitle "Test 22: Publish minor version from ant"

cd $testdir/publisher
echo "2.1" > build/version.txt
ant publish-minor < /dev/null

[ -r $artifacts/supernanny-test-2.1.tar.bz2 ] ||
fail "Test 22: Artifact supernanny-test-2.1.tar.bz2 not created"

colorPass "Test 22 passed OK"


###########
# Test 23

echo
colorTitle "Test 23: Publish major version from ant"

cd $testdir/publisher
echo "3.0" > build/version.txt
ant publish-major < /dev/null

[ -r $artifacts/supernanny-test-3.0.tar.bz2 ] ||
fail "Test 23: Artifact supernanny-test-3.0.tar.bz2 not created"

colorPass "Test 23 passed OK"


###########
# Test 24

echo
colorTitle "Test 24: Fetch dependencies from ant"

cd $testdir/fetcher
rm -rf lib
cat > .DEP <<EOF
supernanny-test TARBZ2 http://localhost:53417/ 2.*
EOF
ant get-deps

[ `cat lib/supernanny-test/version.txt` = "2.1" ] ||
fail "Test 24: Fetched dependency failed or is not version 2.1"

colorPass "Test 24 passed OK"


###########
# Test 25

echo
colorTitle "Test 25: Fetch dependencies from ant with override"

cd $testdir/fetcher
rm -rf lib
mkdir lib
cat > .DEP <<EOF
supernanny-test TARBZ2 http://localhost:53417/ 2.*
EOF
cat > lib/DEP.override <<EOF
supernanny-test TARBZ2 http://localhost:53417/ 1.2
EOF
ant get-deps

[ `cat lib/supernanny-test/version.txt` = "1.2" ] ||
fail "Test 25: Fetched dependency failed or is not version 1.2"

colorPass "Test 25 passed OK"

##########
# Test 26

echo
colorTitle "Test 26: Publish minor version using xz format"

cd $testdir/publisher_xz
echo "3.1" > build/version.txt
java -jar $jar publish -f --next x.+ < /dev/null

[ -r $artifacts/supernanny-test-3.1.tar.xz ] ||
fail "Test 1: Artifact supernanny-test-3.1.tar.xz not created"

colorPass "Test 26 passed OK"

##########
# Test 27

fetchtest 27 "Fetch artifact independent of concrete format" TARBZ2 "3.1" "3.1"

###########
# Test 28

echo
colorTitle "Test 28: Fetch lib cleanup"

cd $testdir/fetcher
rm -rf lib
mkdir lib
# create some temp files/dirs
mkdir lib/futi
mkdir tmpdir
ln -s ../tmpdir lib/link
touch lib/somefile
cat > .DEP <<EOF
supernanny-test TARBZ2 http://localhost:53417/ 1.2
EOF
ant get-deps

[ -d lib/futi ] &&
fail "Test 28: other directories in lib should be deleted"
[ -h lib/link ] ||
fail "Test 28: symlinks should be preserved"
[ -f lib/somefile ] ||
fail "Test 28: plain files should be preserved"

colorPass "Test 28 passed OK"

###########
# Test 29

echo
colorTitle "Test 29: Fetch lib cleanup"

cd $testdir/fetcher
rm -rf lib
mkdir lib
mkdir lib/futi
cat > .DEP <<EOF
supernanny-test TARBZ2 http://localhost:53417/ 1.2
EOF
ant get-deps-partial

[ -d lib/futi ] ||
fail "Test 29: other directories in lib should have been preserved"

colorPass "Test 29 passed OK"


#################
# Final cleanup

echo
colorPass "All tests passed. Good!!"
cleanup
exit 0
