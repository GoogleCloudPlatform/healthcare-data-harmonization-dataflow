#!/bin/bash

set -u -e

WORK_DIR=""
CURR_DIR="$(pwd)"
WORK_DIR="${CURR_DIR}/build/tmp/build_deps"

print_usage() {
  echo "Usage:"
  echo "  build_deps.sh --work_dir <WORK DIR>"
}

while (( "$#" )); do
  if [[ "$1" == "--work_dir" ]]; then
    WORK_DIR="$2"
  else
    echo "Error: unknown flag $1."
    print_usage
    exit 1
  fi
  shift 2
done


# Clean up the workspace
rm -rf "${WORK_DIR}"
mkdir -p "${WORK_DIR}"
rm -rf "${CURR_DIR}/lib"
mkdir -p "${CURR_DIR}/lib"


# At this point, we require OpenJDK 8.
readonly JNI_DIR="/usr/lib/jvm/java-11-openjdk-amd64/include"
readonly JNI_DIR_LINUX="/usr/lib/jvm/java-11-openjdk-amd64/include/linux"

# Data harmonization repo.
readonly DH_REPO="${DH_REPO:-https://github.com/GoogleCloudPlatform/healthcare-data-harmonization.git}"

if [[ ! -d "${JNI_DIR}" || ! -d "${JNI_DIR_LINUX}" ]]; then
  echo "Please make sure OpenJDK 11 is installed. On Debian/Ubuntu, run: sudo apt install openjdk-8-jdk"
  exit 1
fi


REPO_DIR="$(dirname "$0")"

echo "Cloning latest mapping engine code..."
git clone "${DH_REPO}" "${WORK_DIR}"

echo "Building mapping engine..."

# Build whistle-2 runtime,proto,transpiler libraries
cd "${WORK_DIR}"
gradle build
cp "${WORK_DIR}/runtime/build/libs/runtime-dev-SNAPSHOT.jar" "${CURR_DIR}/lib/runtime.jar"
cp "${WORK_DIR}/proto/build/libs/proto-dev-SNAPSHOT.jar" "${CURR_DIR}/lib/proto.jar"
cp "${WORK_DIR}/transpiler/build/libs/transpiler-dev-SNAPSHOT.jar" "${CURR_DIR}/lib/transpiler.jar"

# Clean up work directory
echo "Cleaning up temporary work directory..."
rm -rf "${WORK_DIR}"

cd "${CURR_DIR}"