gdb-multiarch -q --nh \
    -ex 'set architecture aarch64' \
    -ex "file $1" \
    -ex 'target remote localhost:1234' \
    -ex 'layout split' \
    -ex 'layout regs'