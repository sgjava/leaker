/*
 * Copyright (c) Steven P. Goldsmith. All rights reserved.
 */
package com.codeferm.com;

import com.github.kokorin.jaffree.StreamType;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.FFmpegResultFuture;
import com.github.kokorin.jaffree.ffmpeg.Frame;
import com.github.kokorin.jaffree.ffmpeg.FrameConsumer;
import com.github.kokorin.jaffree.ffmpeg.FrameOutput;
import com.github.kokorin.jaffree.ffmpeg.Stream;
import com.github.kokorin.jaffree.ffmpeg.UrlInput;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

/**
 * FrameConsumer leak example.
 *
 * @author Steven P. Goldsmith
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
public class FileLeaker implements FrameConsumer {

    /**
     * Future to run ffmpeg in background.
     */
    private FFmpegResultFuture future;
    /**
     * Frame counter.
     */
    private AtomicInteger frames = new AtomicInteger();

    /**
     * Future is used to run FFMPEG in the background. Disable all streams except video.
     *
     * @param bin FFMPEG binary.
     * @param device String representation of device.
     * @param timeout Connection timeout in milliseconds.
     */
    public void open(final String bin, final String device, final int timeout) {
        // Change input arguments as needed 
        final var input = UrlInput.fromUrl(device);
        // Disable audio, subtitles and data from stream
        final var output = FrameOutput.withConsumer(this).disableStream(StreamType.AUDIO).disableStream(StreamType.SUBTITLE).
                disableStream(StreamType.DATA);
        future = FFmpeg.atPath(Paths.get(bin)).addInput(input).addOutput(output).executeAsync();
        // Wait for first frame
        while (frames.get() == 0) {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        frames.set(0);
    }

    @Override
    public void consumeStreams(List<Stream> streams) {
        // All stream types except video are disabled. just ignore
    }

    /**
     * Get frame as image and check for null.
     *
     * @param frame Frame consumed.
     */
    @Override
    public void consume(final Frame frame) {
        // End of Stream?
        if (frame != null) {
            final var image = frame.getImage();
            if (image != null) {
                frames.incrementAndGet();
            } else {
                throw new RuntimeException("Image is null");
            }
        } else {
            throw new RuntimeException("End of stream");
        }
    }

    /**
     * Shut down ffmpeg.
     */
    public void close() {
        future.graceStop();
    }

    /**
     * Allow consume to run.
     *
     * @param count Iterations.
     */
    public void loop(final int count) {
        var i = 0;
        var check = Instant.now().plusSeconds(10);
        while (i < count) {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            i++;
            if (check.compareTo(Instant.now()) < 0) {
                check = Instant.now().plusSeconds(10);
                log.debug(String.format("%d FPS", frames.get() / 10));
                frames.set(0);
            }
        }
    }

    /**
     * args[0] = URL.
     *
     * @param args Main arguments.
     */
    public static void main(String[] args) {
        var file = "/home/servadmin/Downloads/144357-main.mkv";
        if (args.length > 0) {
            file = args[0];
        }
        final var leaker = new FileLeaker();
        leaker.open("/usr/bin/", file, 10000);
        leaker.loop(660);
        leaker.close();
    }
}
