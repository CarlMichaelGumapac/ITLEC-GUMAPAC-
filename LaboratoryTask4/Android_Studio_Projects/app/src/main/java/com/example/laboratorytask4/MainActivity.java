package com.example.laboratorytask4;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.appcompat.app.AlertDialog;
import android.widget.*;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private EditText etTitle, etContent;
    private Button btnSave;
    private ListView listNotes;

    private List<Note> notesList; // STORAGE OF NOTES OBJECTS

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etTitle = findViewById(R.id.et_title);
        etContent = findViewById(R.id.et_content);
        btnSave = findViewById(R.id.btn_save);
        listNotes = findViewById(R.id.list_notes);

        dbHelper = new DatabaseHelper(this);

        btnSave.setOnClickListener(v -> saveNote());

        loadNotes();

        // --------------------------
        // SINGLE CLICK -> EDIT NOTE
        // --------------------------
        listNotes.setOnItemClickListener((parent, view, position, id) -> {
            Note note = notesList.get(position);

            // Load into input fields for editing
            etTitle.setText(note.title);
            etContent.setText(note.content);

            // Replace save button to update
            btnSave.setText("Update Note");

            btnSave.setOnClickListener(v -> {
                updateNote(note.id);
            });
        });


        // --------------------------
        // LONG CLICK -> MENU (EDIT / DELETE)
        // --------------------------
        listNotes.setOnItemLongClickListener((parent, view, position, id) -> {
            Note note = notesList.get(position);

            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Choose Action")
                    .setItems(new CharSequence[]{"Edit", "Delete"}, (dialog, which) -> {
                        if (which == 0) {
                            // EDIT SELECTED
                            etTitle.setText(note.title);
                            etContent.setText(note.content);
                            btnSave.setText("Update Note");

                            btnSave.setOnClickListener(v -> {
                                updateNote(note.id);
                            });

                        } else if (which == 1) {
                            // DELETE SELECTED
                            deleteNote(note);
                        }
                    })
                    .show();

            return true;
        });
    }

    private void deleteNote(Note note) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Note?")
                .setMessage("Are you sure you want to delete this note?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (dbHelper.onDelete(note.id)) {
                        Toast.makeText(this, "Note deleted", Toast.LENGTH_SHORT).show();
                        loadNotes();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateNote(long id) {
        String newTitle = etTitle.getText().toString().trim();
        String newContent = etContent.getText().toString().trim();

        if (newTitle.isEmpty()) {
            Toast.makeText(this, "Title cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        if (dbHelper.updateNote(id, newTitle, newContent)) {
            Toast.makeText(this, "Note updated!", Toast.LENGTH_SHORT).show();
            etTitle.setText("");
            etContent.setText("");
            btnSave.setText("Save Note");

            btnSave.setOnClickListener(v -> saveNote()); // restore original behavior
            loadNotes();
        } else {
            Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveNote() {
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a title", Toast.LENGTH_SHORT).show();
            return;
        }

        long id = dbHelper.insertNote(title, content);
        if (id != -1) {
            Toast.makeText(this, "Note saved!", Toast.LENGTH_SHORT).show();
            etTitle.setText("");
            etContent.setText("");
            loadNotes();
        } else {
            Toast.makeText(this, "Failed to save note", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadNotes() {
        notesList = dbHelper.getAllNotes();
        ArrayAdapter<Note> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, notesList);
        listNotes.setAdapter(adapter);
    }
}
