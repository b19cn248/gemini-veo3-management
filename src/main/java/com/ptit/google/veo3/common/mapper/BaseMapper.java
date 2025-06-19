package com.ptit.google.veo3.common.mapper;

import java.util.List;

public interface BaseMapper<E, D> {
    
    D toDto(E entity);
    
    E toEntity(D dto);
    
    List<D> toDtoList(List<E> entities);
    
    List<E> toEntityList(List<D> dtos);
    
    void updateEntityFromDto(D dto, E entity);
}