#!/bin/bash

#set -x

# ENV VAR below are required to be set in the build environment. 
#$SIGNPATH_USER_TOKEN - SignPath user token, this is the CI user created in SignPath
#$SIGNPATH_PROJECT - Project Slug settings from SignPath
#$SIGNPATH_POLICY - Policy Slug settings from SignPath
#$SIGNPATH_CONFIGURATION - Configuration Slug settings from SignPath
#$SIGNPATH_ORG - SignPath organization ID

#$1 path of the file to be signed
response=$(curl -sSL -D - -o /dev/null -H "Authorization: Bearer $SIGNPATH_USER_TOKEN" \
     -F "ProjectSlug=$SIGNPATH_PROJECT" \
     -F "SigningPolicySlug=$SIGNPATH_POLICY" \
     -F "ArtifactConfigurationSlug=$SIGNPATH_CONFIGURATION" \
     -F "Artifact=@$1" \
     https://app.signpath.io/API/v1/$SIGNPATH_ORG/SigningRequests)

http_status=$(echo "$response" | grep HTTP | awk '{print $2}')
location_header=$(echo "$response" | grep Location | awk '{print $2}' | tr -d '\r')

echo "Going to ping request status at $location_header"

if [[ $response == *"201"* ]]; then
  json_status=""
  json_response=""
  while [[ "$json_status" != "Completed" && "$json_status" != "Failed" && "$json_status" != "Cancelled" && "$json_status" != "Denied" ]]
    do
    json_response=$(curl -sSL -H "Authorization: Bearer $SIGNPATH_USER_TOKEN" $location_header)
    json_status=$(echo $json_response | jq .status | tr -d '"')
    sleep 5
    echo json status $json_status
  done

  if [[ $json_status == "Completed" ]]; then
    mv $1 "$1.unsigned"
    json_signed_artifact_link=$(echo $json_response | jq .signedArtifactLink | tr -d '"')
    echo Signed artifact link $json_signed_artifact_link
    echo Downloading signed file to $1

    download_return_code=0
    download_status_code=$(curl -s -o $1 -w "%{http_code}" -H "Authorization: Bearer $SIGNPATH_USER_TOKEN" $json_signed_artifact_link) || download_return_code=$?
    if [[ $download_return_code != 0 ]]; then  
        echo "Curl connection failed with return code - $download_return_code"
        exit 3
    else
        if [[ "$download_status_code" != 200 ]]; then
            echo "Curl operation/command failed due to server return code - $download_status_code"
            exit 4
        else 
            echo "Curl success with status code $download_status_code"
        fi
    fi


  else 
    echo Failed with json status $json_status 
    exit 2
  fi

else
  echo Invalid Status code $http_status
  exit 1
fi


