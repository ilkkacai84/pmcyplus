package com.rcai.pm.project;

import java.io.Serializable;

public record TaskParticipantId(Long taskId, Long userId) implements Serializable {}
