#!/bin/sh

echo "Generating java..."
thrift -gen java Sample.thrift
