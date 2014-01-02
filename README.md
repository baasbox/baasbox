BaasBox
=======

BaasBox is an Open Source project that aims to provide a general purpose back end for mobile and web apps.

Further information can be found at [www.baasbox.com](http://www.baasbox.com/ "BaasBox site").

Build BaasBox
-------------
### Prerequisites
To build BaasBox you need a JDK (not JRE!) (version 6 or above) ([download here](http://www.oracle.com/technetwork/java/javase/downloads/index.html)) and the Play! Framework 2.1.1 ([download here](http://www.playframework.org/download))

Once you have installed the above software following, you will be able to build BaasBox.

Download the source code from [GitHub](https://github.com/baasbox/baasbox)

### Build
Go to your local BaasBox source code directory and type:

`play dist`

Play! will build BaasBox and will create a .zip file in the ./dist directory.

The .zip file contains everything you need to run BaasBox.
Unzip it, set the execution permission on the start.sh file and run it.
BaasBox will create a new database in the ./db directory, and it will start.

To test it visit <http://localhost:9000> and you will see the BaasBox start page.

To access the **Admin Console Panel**, go to <http://localhost:9000/console>

Default credentials are:

+ username: admin
+ passwrod: admin
+ application code: 1234567890


[![Bitdeli Badge](https://d2weczhvl823v0.cloudfront.net/baasbox/baasbox/trend.png)](https://bitdeli.com/free "Bitdeli Badge")

