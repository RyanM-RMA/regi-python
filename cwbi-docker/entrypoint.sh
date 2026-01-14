#!/bin/bash

## Check if OFFICE is set
#if [ -z "$OFFICE" ]; then
#  echo "OFFICE is not set"
#  exit 1
#fi
#
## Lowercase the OFFICE variable
#OFFICE_LOWER="${OFFICE,,}"
#
## Check if GITHUB_TOKEN is set
#if [ -z "$GITHUB_TOKEN" ]; then
#  echo "GITHUB_TOKEN is not set"
#  exit 1
#fi
#
#if [ -z "$ENVIRONMENT" ]; then
#  echo "ENVIRONMENT not set"
#  exit 1
#fi
#
#GITHUB_BRANCH="cwbi-$ENVIRONMENT"
#
#echo "Using GITHUB_BRANCH: $GITHUB_BRANCH"
#
## Clone the office repo
#git clone --branch $GITHUB_BRANCH \
#    https://$GITHUB_TOKEN@github.com/USACE-WaterManagement/$OFFICE_LOWER-wm-cwbi-jobs.git /jobs
#if [ $? -ne 0 ]; then
#  echo "Failed to clone the repository for $OFFICE"
#  exit 1
#fi
#
#unset GITHUB_TOKEN

# Set all shell scripts to executable
chmod +x /jobs/bin/*.sh


# Check if the requirements.txt file exists and install Python dependencies
REQUIREMENTS_FILE="/jobs/python/requirements.txt"

if [ -f "$REQUIREMENTS_FILE" ]; then
  echo "Installing Python dependencies from $REQUIREMENTS_FILE..."
  pip install -r "$REQUIREMENTS_FILE"
else
  echo "No requirements.txt found at $REQUIREMENTS_FILE"
fi

if [ -d "/app/local_dist" ]; then
    echo "Installing local wheels from /app/local_dist..."
    pip install --force-reinstall /app/local_dist/*.whl
fi

# Run whatever CMD was passed in
exec "$@"
