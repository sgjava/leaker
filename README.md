Leaker demonstrates how Jaffree leaks FFMPEG memory using FrameConsumer. Modify
main method of Leaker class to point to video stream. Also set loop to number of
seconds to run. [Procpath](https://pypi.org/project/Procpath) can be used to
track memory usage by pid.

* `ps -ef | grep ffmpeg`
* `procpath record -i 60 -r 10 -d ff.sqlite -p 123`
* `procpath plot -d ff.sqlite -q rss -p 123 -f rss.svg`

![Leak](images/rss.svg)