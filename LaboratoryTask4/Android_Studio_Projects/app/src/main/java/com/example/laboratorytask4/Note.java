package com.example.laboratorytask4;

public class Note {
    public long id;
    public String title;
    public String content;

    public Note(long id, String title, String content) {
        this.id = id;
        this.title = title;
        this.content = content;
    }

    @Override
    public String toString() {
        return title + " - " + content;
    }
}
