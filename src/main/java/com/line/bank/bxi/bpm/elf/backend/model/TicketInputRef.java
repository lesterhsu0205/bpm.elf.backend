package com.line.bank.bxi.bpm.elf.backend.model;

public class TicketInputRef {
    private int ticketIndex;
    private int inputIndex;

    public TicketInputRef(int ticketIndex, int inputIndex) {
        this.ticketIndex = ticketIndex;
        this.inputIndex = inputIndex;
    }

    public int getTicketIndex() {
        return ticketIndex;
    }

    public int getInputIndex() {
        return inputIndex;
    }
}
