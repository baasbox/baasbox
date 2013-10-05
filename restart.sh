# /bin/bash                                                                        

z=$(\rm -rf db/)
echo "deleted db"
a=$(pidof java)
kill $a
echo "killed java"
echo "restarting baasbox"
./start &