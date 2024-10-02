This is the installer script for linux system. 

To update the script on production:
1. copy (manually) the script to https://console.aws.amazon.com/s3/object/rc-files-t2/appoptics-java.sh?region=us-west-2&tab=overview
2. run `deploy-prod` job on jenkins and use appoptics-java.sh as 'COPY_PATH'
