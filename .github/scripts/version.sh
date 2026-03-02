VERSION=$(grep 'version=' "$@" | awk -F= '{ print $2 }')
echo "$VERSION"