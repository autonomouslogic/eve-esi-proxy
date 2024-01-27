#!/bin/bash

bin/esi-proxy
ex=$?

if compgen -G "/tmp/hs_err_pid*.log" > /dev/null; then
    cat /tmp/hs_err_pid*.log
fi

exit $ex
