package ru.avem.resonance.states.experiment;

public interface Statable {
    void toInitState();

    void toRunState();

    void toStoppingState();

    void toErrorState();

    void toFinishedState();
}
