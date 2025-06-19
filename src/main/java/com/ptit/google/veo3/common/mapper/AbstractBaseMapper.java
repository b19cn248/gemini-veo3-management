package com.ptit.google.veo3.common.mapper;

import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractBaseMapper<E, D> implements BaseMapper<E, D> {
    
    @Override
    public List<D> toDtoList(List<E> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<E> toEntityList(List<D> dtos) {
        if (dtos == null) {
            return null;
        }
        return dtos.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }
}