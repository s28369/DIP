package org.example.fleetmanagement.service;

import org.example.fleetmanagement.model.Trailer;
import org.example.fleetmanagement.model.TrailerNote;
import org.example.fleetmanagement.repository.TrailerNoteRepository;
import org.example.fleetmanagement.repository.TrailerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for trailer (semi-trailer) operations and their notes.
 */
@Service
@Transactional
public class TrailerService {

    private final TrailerRepository trailerRepository;
    private final TrailerNoteRepository noteRepository;

    // Constructor injection of the trailer and trailer-note repositories.
    @Autowired
    public TrailerService(TrailerRepository trailerRepository, TrailerNoteRepository noteRepository) {
        this.trailerRepository = trailerRepository;
        this.noteRepository = noteRepository;
    }

    // Returns all trailers with their related details loaded.
    public List<Trailer> getAllTrailers() {
        return trailerRepository.findAllWithDetails();
    }

    // Finds a single trailer by id with its details loaded.
    public Optional<Trailer> getTrailerById(Long id) {
        return trailerRepository.findByIdWithDetails(id);
    }

    // Adds a new trailer, rejecting duplicate registration numbers.
    public Trailer addTrailer(Trailer trailer) {
        if (trailerRepository.existsByRegistrationNumber(trailer.getRegistrationNumber())) {
            throw new IllegalArgumentException(
                "Naczepa o numerze " + trailer.getRegistrationNumber() + " już istnieje");
        }
        return trailerRepository.save(trailer);
    }

    // Persists changes to an existing trailer.
    public Trailer updateTrailer(Trailer trailer) {
        return trailerRepository.save(trailer);
    }

    // Deletes a trailer by id, failing if it does not exist.
    public void deleteTrailer(Long id) {
        if (!trailerRepository.existsById(id)) {
            throw new IllegalArgumentException("Naczepa o ID " + id + " nie istnieje");
        }
        trailerRepository.deleteById(id);
    }

    // Returns trailers filtered by status.
    public List<Trailer> getTrailersByStatus(String status) {
        return trailerRepository.findByStatus(status);
    }

    // --- Notes ---

    // Returns the notes of a trailer, newest first.
    public List<TrailerNote> getNotesByTrailer(Long trailerId) {
        return noteRepository.findByTrailerIdOrderByCreatedAtDesc(trailerId);
    }

    // Saves a new note attached to a trailer.
    public TrailerNote addNote(TrailerNote note) {
        return noteRepository.save(note);
    }

    // Persists changes to an existing trailer note.
    public TrailerNote updateNote(TrailerNote note) {
        return noteRepository.save(note);
    }

    // Deletes a trailer note by id.
    public void deleteNote(Long noteId) {
        noteRepository.deleteById(noteId);
    }
}
