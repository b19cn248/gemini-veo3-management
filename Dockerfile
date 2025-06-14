# Sử dụng OpenJDK 21 slim image làm base image
FROM openjdk:21-jdk-slim

# Metadata
LABEL maintainer="video-management-team"
LABEL description="Video Management API with Spring Boot"
LABEL version="1.0"

# Cài đặt các dependencies cần thiết và set timezone
RUN apt-get update && apt-get install -y \
    curl \
    wget \
    tzdata \
    && rm -rf /var/lib/apt/lists/*

# Set timezone to Asia/Ho_Chi_Minh
ENV TZ=Asia/Ho_Chi_Minh
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Tạo user non-root để chạy application (security best practice)
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Tạo thư mục làm việc
WORKDIR /app

# Copy Maven wrapper và pom.xml trước để tận dụng Docker layer caching
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Cấp quyền thực thi cho Maven wrapper
RUN chmod +x ./mvnw

# Download dependencies trước (tận dụng Docker layer caching)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Build application
RUN ./mvnw clean package -DskipTests -B

# Tạo thư mục cho logs
RUN mkdir -p /app/logs

# Chuyển ownership cho appuser
RUN chown -R appuser:appuser /app

# Chuyển sang user non-root
USER appuser

# Expose port 8080
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Cấu hình JVM options cho production và timezone
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:G1HeapRegionSize=16m -XX:+UseStringDeduplication -Duser.timezone=Asia/Ho_Chi_Minh"

# Command để chạy application
CMD ["sh", "-c", "java $JAVA_OPTS -jar target/gemini-veo3-management-*.jar"]