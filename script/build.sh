#!/bin/bash

CUR_DIR=$(cd `dirname $0`;pwd)

cd $CUR_DIR

# update library
cp -R ../node_modules/android-pc-communication/android/pc ../FreekiteAndroidWebViewClient

# build
pushd ../FreekiteAndroidWebViewClient
./gradlew build
popd
