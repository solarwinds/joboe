package com.solarwinds.joboe.logging;

class CompositeStream implements LoggerStream {
        private final LoggerStream[] streams;

        CompositeStream(LoggerStream... streams) {
            this.streams = streams;
        }

        @Override
        public void println(String value) {
            for (LoggerStream stream : streams) {
                stream.println(value);
            }
        }

        @Override
        public void printStackTrace(Throwable throwable) {
            for (LoggerStream stream : streams) {
                stream.printStackTrace(throwable);
            }

        }

        LoggerStream[] getStreams() {
            return streams;
        }
    }