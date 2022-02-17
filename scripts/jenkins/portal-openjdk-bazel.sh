# Copyright 2021 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

### Dockerization of Manager ####### Doc
set -x
set -e

SCRIPT_DIR="$(dirname $0)"
source "${SCRIPT_DIR}/portal-openjdk-bazel-commons.sh"

prepare_to_copy_jars

copy_cg_manager_jars

mkdir -p dist/platform-service
cd dist/platform-service

cp ${HOME}/.bazel-dirs/bin/820-platform-service/module_deploy.jar platform-service-capsule.jar
cp ../../820-platform-service/config.yml .
cp ../../820-platform-service/keystore.jks .
cp ../../820-platform-service/key.pem .
cp ../../820-platform-service/cert.pem .
cp ../../dockerization/platform-service/Dockerfile-platform-service-jenkins-k8-openjdk ./Dockerfile
cp ../../dockerization/platform-service/Dockerfile-platform-service-jenkins-k8-gcr-openjdk ./Dockerfile-gcr
cp ../../dockerization/build/Dockerfile-jenkins-slave-portal-jdk-11 ./Dockerfile-jenkins
cp -r ../../dockerization/platform-service/scripts .
cp ../../protocol.info .
echo ${JDK} > jdk.txt
echo ${VERSION} > version.txt
if [ ! -z ${PURPOSE} ]
then
    echo ${PURPOSE} > purpose.txt
fi
#java -jar platform-service-capsule.jar scan-classpath-metadata

cd ../..

mkdir -p dist/accesscontrol-service
cd dist/accesscontrol-service

cp ${HOME}/.bazel-dirs/bin/access-control/service/module_deploy.jar accesscontrol-service-capsule.jar
cp ../../access-control/service/config.yml .
cp ../../access-control/service/keystore.jks .
cp ../../access-control/container/Dockerfile-accesscontrol-service-jenkins-k8-openjdk ./Dockerfile
cp ../../access-control/container/Dockerfile-accesscontrol-service-jenkins-k8-gcr-openjdk ./Dockerfile-gcr
cp -r ../../access-control/container/scripts/ .
cp ../../protocol.info .
echo ${JDK} > jdk.txt
echo ${VERSION} > version.txt
if [ ! -z ${PURPOSE} ]
then
    echo ${PURPOSE} > purpose.txt
fi
#java -jar accesscontrol-service-capsule.jar scan-classpath-metadata

cd ../..

mkdir -p dist/pipeline-service
cd dist/pipeline-service

cp ${HOME}/.bazel-dirs/bin/800-pipeline-service/module_deploy.jar pipeline-service-capsule.jar
cp ../../800-pipeline-service/config.yml .
cp ../../800-pipeline-service/keystore.jks .
cp ../../800-pipeline-service/key.pem .
cp ../../800-pipeline-service/cert.pem .
cp ../../800-pipeline-service/src/main/resources/redisson-jcache.yaml .

cp ../../alpn-boot-8.1.13.v20181017.jar .
cp ../../dockerization/pipeline-service/Dockerfile-pipeline-service-jenkins-k8-openjdk ./Dockerfile
cp ../../dockerization/pipeline-service/Dockerfile-pipeline-service-jenkins-k8-gcr-openjdk ./Dockerfile-gcr
cp -r ../../dockerization/pipeline-service/scripts/ .
cp ../../pipeline-service-protocol.info .
echo ${JDK} > jdk.txt
echo ${VERSION} > version.txt
if [ ! -z ${PURPOSE} ]
then
    echo ${PURPOSE} > purpose.txt
fi
java -jar pipeline-service-capsule.jar scan-classpath-metadata

cd ../..

mkdir -p dist/debezium-service
cd dist/debezium-service

cp ${HOME}/.bazel-dirs/bin/951-debezium-service/module_deploy.jar debezium-service-capsule.jar
cp ../../951-debezium-service/config.yml .
cp ../../951-debezium-service/src/main/resources/redisson-jcache.yaml .

cp ../../alpn-boot-8.1.13.v20181017.jar .
cp ../../dockerization/debezium-service/Dockerfile-debezium-service-jenkins-k8-openjdk ./Dockerfile
cp ../../dockerization/debezium-service/Dockerfile-debezium-service-jenkins-k8-gcr-openjdk ./Dockerfile-gcr
cp -r ../../dockerization/debezium-service/scripts/ .
cp ../../protocol.info .
echo ${JDK} > jdk.txt
echo ${VERSION} > version.txt
if [ ! -z ${PURPOSE} ]
then
    echo ${PURPOSE} > purpose.txt
fi

cd ../..

mkdir -p dist/template-service
cd dist/template-service

cp ${HOME}/.bazel-dirs/bin/840-template-service/module_deploy.jar template-service-capsule.jar
cp ../../840-template-service/config.yml .
cp ../../840-template-service/keystore.jks .
cp ../../840-template-service/key.pem .
cp ../../840-template-service/cert.pem .
cp ../../840-template-service/src/main/resources/redisson-jcache.yaml .

cp ../../alpn-boot-8.1.13.v20181017.jar .
cp ../../dockerization/template-service/Dockerfile-template-service-jenkins-k8-openjdk ./Dockerfile
cp ../../dockerization/template-service/Dockerfile-template-service-jenkins-k8-gcr-openjdk ./Dockerfile-gcr
cp -r ../../dockerization/template-service/scripts/ .
cp ../../protocol.info .
echo ${JDK} > jdk.txt
echo ${VERSION} > version.txt
if [ ! -z ${PURPOSE} ]
then
    echo ${PURPOSE} > purpose.txt
fi

cd ../..

mkdir -p dist/eventsapi-monitor
cd dist/eventsapi-monitor

cp ${HOME}/.bazel-dirs/bin/950-events-framework-monitor/module_deploy.jar eventsapi-monitor-capsule.jar
cp ../../950-events-framework-monitor/config.yml .
cp ../../950-events-framework-monitor/redis/* .
cp ../../alpn-boot-8.1.13.v20181017.jar .
cp ../../dockerization/eventsapi-monitor/Dockerfile-eventsapi-monitor-jenkins-k8-openjdk ./Dockerfile
cp ../../dockerization/eventsapi-monitor/Dockerfile-eventsapi-monitor-jenkins-k8-gcr-openjdk ./Dockerfile-gcr
cp -r ../../dockerization/eventsapi-monitor/scripts/ .
cp ../../protocol.info .
echo ${JDK} > jdk.txt
echo ${VERSION} > version.txt
if [ ! -z ${PURPOSE} ]
then
    echo ${PURPOSE} > purpose.txt
fi

cd ../..

mkdir -p dist/accesscontrol-service
cd dist/accesscontrol-service

cp ${HOME}/.bazel-dirs/bin/access-control/service/module_deploy.jar accesscontrol-service-capsule.jar
cp ../../access-control/service/config.yml .
cp ../../access-control/service/keystore.jks .
cp ../../alpn-boot-8.1.13.v20181017.jar .
cp ../../access-control/container/Dockerfile-accesscontrol-service-jenkins-k8-openjdk ./Dockerfile
cp ../../access-control/container/Dockerfile-accesscontrol-service-jenkins-k8-gcr-openjdk ./Dockerfile-gcr
cp -r ../../access-control/container/scripts/ .
cp ../../protocol.info .
echo ${JDK} > jdk.txt
echo ${VERSION} > version.txt
if [ ! -z ${PURPOSE} ]
then
    echo ${PURPOSE} > purpose.txt
fi
java -jar accesscontrol-service-capsule.jar scan-classpath-metadata

cd ../..

mkdir -p dist/migrator ;
cd dist/migrator

cp ${BAZEL_BIN}/100-migrator/module_deploy.jar migrator-capsule.jar
cp ../../400-rest/src/main/resources/hazelcast.xml .
cp ../../keystore.jks .
cp ../../360-cg-manager/key.pem .
cp ../../360-cg-manager/cert.pem .
cp ../../360-cg-manager/newrelic.yml .
cp ../../100-migrator/config.yml .
cp ../../400-rest/src/main/resources/redisson-jcache.yaml .
cp ../../alpn-boot-8.1.13.v20181017.jar .

cp ../../dockerization/migrator/Dockerfile-manager-jenkins-k8-openjdk ./Dockerfile
cp ../../dockerization/migrator/Dockerfile-manager-jenkins-k8-gcr-openjdk ./Dockerfile-gcr
cp -r ../../dockerization/migrator/scripts/ .
mv scripts/start_process_bazel.sh scripts/start_process.sh

copy_common_files

java -jar migrator-capsule.jar scan-classpath-metadata

cd ../..