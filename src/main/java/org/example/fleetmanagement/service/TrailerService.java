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

@Service
@Transactional
public class TrailerService {

    private final TrailerRepository trailerRepository;
    private final TrailerNoteRepository noteRepository;

    @Autowired
    public TrailerService(TrailerRepository trailerRepository, TrailerNoteRepository noteRepository) {
        this.trailerRepository = trailerRepository;
        this.noteRepository = noteRepository;
    }

    public List<Trailer> getAllTrailers() {
        return trailerRepository.findAll();
    }

    public Optional<Trailer> getTrailerById(Long id) {
        return trailerRepository.findById(id);
    }

    public Trailer addTrailer(Trailer trailer) {
        if (trailerRepository.existsByRegistrationNumber(trailer.getRegistrationNumber())) {
            throw new IllegalArgumentException(
                "Прицеп с номером " + trailer.getRegistrationNumber() + " уже существует");
        }
        return trailerRepository.save(trailer);
    }

    public Trailer updateTrailer(Trailer trailer) {
        return trailerRepository.save(trailer);
    }

    public void deleteTrailer(Long id) {
        if (!trailerRepository.existsById(id)) {
            throw new IllegalArgumentException("Прицеп с ID " + id + " не существует");
        }
        trailerRepository.deleteById(id);
    }

    public List<Trailer> getTrailersByStatus(String status) {
        return trailerRepository.findByStatus(status);
    }

    // --- Notes ---

    public List<TrailerNote> getNotesByTrailer(Long trailerId) {
        return noteRepository.findByTrailerIdOrderByCreatedAtDesc(trailerId);
    }

    public TrailerNote addNote(TrailerNote note) {
        return noteRepository.save(note);
    }

    public TrailerNote updateNote(TrailerNote note) {
        return noteRepository.save(note);
    }

    public void deleteNote(Long noteId) {
        noteRepository.deleteById(noteId);
    }
}
