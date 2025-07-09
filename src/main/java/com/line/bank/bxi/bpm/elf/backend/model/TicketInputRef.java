package com.line.bank.bxi.bpm.elf.backend.model;

public class TicketInputRef {

    private String ticketName;

    public TicketInputRef(String ticketName) {
        this.ticketName = ticketName;
    }

    public String getTicketName() {
        return ticketName;
    }

    public void setTicketName(String ticketName) {
        this.ticketName = ticketName;
    }
}
