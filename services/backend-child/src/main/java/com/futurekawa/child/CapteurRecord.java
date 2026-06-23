package com.futurekawa.child;

import java.time.LocalDate;

public record CapteurRecord(Long id, Double humidite, Double temperature, LocalDate date, Integer idEntrepot) {}
