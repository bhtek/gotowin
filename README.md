# gotowin

## Intention

I want a simple and quick command that I can bind to some extra keys on my keyboard to raise specific windows. I want to use it for quick switch.

## Other goals.

* Tried to do it using Kotlin Native to try and play with it again. Previous attempts years ago did not work well. Enough time has passed to try it again.
* But I only want and need it to work on my Linux box only, so this only compiles on Linux.

## Inspirations and help in random order.

* https://github.com/zt64/prism/
* https://victor.kropp.name/blog/x11-app-in-kotlin-native/
* https://github.com/wangzhengbo/JWMCtrl
* https://stackoverflow.com/a/66181988/535761
* https://tronche.com/gui/x/xlib/window/stacking-order.html#:~:text=Xlib%20provides%20functions%20that%20you,or%20down%2C%20use%20XCirculateSubwindows().

and many more that I did not keep track off while getting this to work.

## Remaining work.

* Get window class and be able to filter for it.
* Implement some simple command line parameters so I can specify filters from command line.
