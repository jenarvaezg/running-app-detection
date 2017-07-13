#!/bin/bash

kill  `ps | grep python | sed -e 's/^[[:space:]]*//' | tr -s " " | cut -d' ' -f1`
