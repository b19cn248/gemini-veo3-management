package com.ptit.google.veo3.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.data.domain.Page;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaginatedResponse<T> extends ApiResponse<List<T>> {
    private PaginationInfo pagination;

    public PaginatedResponse() {
        super();
    }

    public PaginatedResponse(boolean success, String message, List<T> data, PaginationInfo pagination) {
        super(success, message, data);
        this.pagination = pagination;
    }

    public static <T> PaginatedResponse<T> success(String message, Page<T> page) {
        PaginationInfo paginationInfo = PaginationInfo.fromPage(page);
        return new PaginatedResponse<>(true, message, page.getContent(), paginationInfo);
    }

    public static <T> PaginatedResponse<T> success(Page<T> page) {
        return success("Data retrieved successfully", page);
    }

    public static <T> PaginatedResponse<T> success(String message, List<T> data, PaginationInfo pagination) {
        return new PaginatedResponse<>(true, message, data, pagination);
    }

    public PaginationInfo getPagination() {
        return pagination;
    }

    public void setPagination(PaginationInfo pagination) {
        this.pagination = pagination;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PaginationInfo {
        private int currentPage;
        private int totalPages;
        private long totalElements;
        private int pageSize;
        private boolean hasNext;
        private boolean hasPrevious;
        private boolean isFirst;
        private boolean isLast;

        public static PaginationInfo fromPage(Page<?> page) {
            PaginationInfo info = new PaginationInfo();
            info.currentPage = page.getNumber();
            info.totalPages = page.getTotalPages();
            info.totalElements = page.getTotalElements();
            info.pageSize = page.getSize();
            info.hasNext = page.hasNext();
            info.hasPrevious = page.hasPrevious();
            info.isFirst = page.isFirst();
            info.isLast = page.isLast();
            return info;
        }

        public int getCurrentPage() {
            return currentPage;
        }

        public void setCurrentPage(int currentPage) {
            this.currentPage = currentPage;
        }

        public int getTotalPages() {
            return totalPages;
        }

        public void setTotalPages(int totalPages) {
            this.totalPages = totalPages;
        }

        public long getTotalElements() {
            return totalElements;
        }

        public void setTotalElements(long totalElements) {
            this.totalElements = totalElements;
        }

        public int getPageSize() {
            return pageSize;
        }

        public void setPageSize(int pageSize) {
            this.pageSize = pageSize;
        }

        public boolean isHasNext() {
            return hasNext;
        }

        public void setHasNext(boolean hasNext) {
            this.hasNext = hasNext;
        }

        public boolean isHasPrevious() {
            return hasPrevious;
        }

        public void setHasPrevious(boolean hasPrevious) {
            this.hasPrevious = hasPrevious;
        }

        public boolean isFirst() {
            return isFirst;
        }

        public void setFirst(boolean first) {
            isFirst = first;
        }

        public boolean isLast() {
            return isLast;
        }

        public void setLast(boolean last) {
            isLast = last;
        }
    }
}