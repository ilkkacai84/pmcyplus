package com.rcai.pm.project;

import java.io.Serializable;

public record ProjectMemberId(Long projectId, Long userId) implements Serializable {}
