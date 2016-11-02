#!/bin/bash

CUR_DIR=$(cd `dirname $0`;pwd)

cd $CUR_DIR

pushd ./testBridge
webpack
popd

cp -R ./testBridge/lib/ ../FreekiteAndroidWebViewClient/app/src/main/assets/test3
