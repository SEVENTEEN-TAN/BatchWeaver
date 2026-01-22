#!/bin/bash

################################################################################
# BatchWeaver All Jobs Runner - Shell Script
# 功能：通过 Maven 运行所有 Spring Batch Job
# 用法：./run-all-jobs.sh [options]
################################################################################

set -e  # 遇到错误立即退出，但我们会手动处理 Job 失败

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
WHITE='\033[0;37m'
NC='\033[0m' # No Color

# 默认 Job 列表
JOBS=(
    "demoJob"
    "conditionalFlowJob"
    "chunkProcessingJob"
    "format1ImportJob"
    "format2ImportJob"
    "format3ImportJob"
    "masterImportJob"
    "format1ExportJob"
    "format2ExportJob"
    "complexWorkflowJob"
)

# 默认参数
LOG_FILE="out.log"
SKIP_TESTS=true

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# 帮助信息
function show_help() {
    echo "用法: $0 [options]"
    echo ""
    echo "选项:"
    echo "  -l, --log FILE       日志文件名 (默认: out.log)"
    echo "  -j, --jobs JOB1,JOB2 指定要运行的 Job (逗号分隔)"
    echo "  -t, --with-tests     不跳过测试"
    echo "  -h, --help          显示此帮助信息"
    echo ""
    echo "可用的 Job:"
    echo "  demoJob                  基础文件导入"
    echo "  conditionalFlowJob      条件分支 (根据 skip 数量)"
    echo "  chunkProcessingJob      数据校验 + 清理流程"
    echo "  complexWorkflowJob      多步骤 + 邮件通知"
    echo "  masterImportJob         串行执行多个格式导入"
    echo "  format1ImportJob        格式1文件导入"
    echo "  format2ImportJob        格式2文件导入"
    echo "  format3ImportJob        格式3文件导入"
    echo "  format1ExportJob        数据导出 (格式1)"
    echo "  format2ExportJob        数据导出 (格式2)"
    echo ""
    echo "示例:"
    echo "  $0                          # 运行所有 Job"
    echo "  $0 -j demoJob,chunkProcessingJob"
    echo "  $0 -l my.log"
    echo "  $0 --with-tests"
}

# 日志函数
function log() {
    local level=$1
    shift
    local message="$@"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')

    case $level in
        INFO)
            echo -e "${CYAN}[INFO]${NC} $message"
            echo "[$timestamp] [INFO] $message" >> "$LOG_FILE"
            ;;
        SUCCESS)
            echo -e "${GREEN}[OK]${NC} $message"
            echo "[$timestamp] [OK] $message" >> "$LOG_FILE"
            ;;
        ERROR)
            echo -e "${RED}[ERROR]${NC} $message"
            echo "[$timestamp] [ERROR] $message" >> "$LOG_FILE"
            ;;
        WARN)
            echo -e "${YELLOW}[WARN]${NC} $message"
            echo "[$timestamp] [WARN] $message" >> "$LOG_FILE"
            ;;
        JOB)
            echo -e "${YELLOW}Running: $message${NC}"
            echo "[$timestamp] [JOB] $message" >> "$LOG_FILE"
            ;;
        HEADER)
            echo -e "${CYAN}$message${NC}"
            echo "[$timestamp] $message" >> "$LOG_FILE"
            ;;
        *)
            echo "$message"
            echo "[$timestamp] $message" >> "$LOG_FILE"
            ;;
    esac
}

# 检查 Maven 是否安装
function check_maven() {
    if ! command -v mvn &> /dev/null; then
        log ERROR "Maven 未安装，请先安装 Maven 并确保 PATH 配置正确"
        exit 1
    fi
}

# 运行 Job
function run_job() {
    local job_name=$1

    log JOB "$job_name"
    echo "" >> "$LOG_FILE"

    # 构建 Maven 命令
    local mvn_cmd="mvn spring-boot:run"
    local args="-DskipTests=true"

    if [ "$SKIP_TESTS" = false ]; then
        args="-DskipTests=false"
    fi

    local job_arg="-Dspring-boot.run.arguments=--job.name=$job_name"

    # 执行 Maven 并同时输出到控制台和日志文件
    if $mvn_cmd $args $job_arg 2>&1 | tee -a "$LOG_FILE"; then
        log SUCCESS "$job_name succeeded"
        echo "" >> "$LOG_FILE"
        return 0
    else
        local exit_code=$?
        log ERROR "$job_name failed (ExitCode=$exit_code)"
        echo "" >> "$LOG_FILE"
        return 1
    fi
}

# 解析命令行参数
while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            show_help
            exit 0
            ;;
        -l|--log)
            LOG_FILE="$2"
            shift 2
            ;;
        -j|--jobs)
            # 将逗号分隔的 Job 列表转换为数组
            IFS=',' read -ra CUSTOM_JOBS <<< "$2"
            JOBS=("${CUSTOM_JOBS[@]}")
            shift 2
            ;;
        -t|--with-tests)
            SKIP_TESTS=false
            shift
            ;;
        -*)
            echo "未知选项: $1"
            show_help
            exit 1
            ;;
        *)
            echo "未知参数: $1"
            show_help
            exit 1
            ;;
    esac
done

# 初始化日志文件
> "$LOG_FILE"

# 开始执行
log HEADER "=========================================="
log HEADER "BatchWeaver Jobs Runner"
log HEADER "=========================================="
log INFO "Start time: $(date '+%Y-%m-%d %H:%M:%S')"
log INFO "Log file: $LOG_FILE"
log INFO "Skip tests: $SKIP_TESTS"
log INFO "Jobs to run: ${#JOBS[@]}"
for job in "${JOBS[@]}"; do
    log INFO "  - $job"
done
log HEADER "=========================================="
echo "" >> "$LOG_FILE"

# 检查 Maven
check_maven

# 运行所有 Job
success=0
failed=0

for job in "${JOBS[@]}"; do
    if run_job "$job"; then
        ((success++))
    else
        ((failed++))
    fi
done

# 打印总结
log HEADER "=========================================="
log HEADER "执行总结"
log HEADER "=========================================="
log INFO "Total: ${#JOBS[@]} | Success: $success | Failed: $failed"
log INFO "End time: $(date '+%Y-%m-%d %H:%M:%S')"
log HEADER "=========================================="
echo ""
echo -e "${CYAN}日志已保存到: $LOG_FILE${NC}"

# 根据执行结果决定退出码
if [ $failed -gt 0 ]; then
    echo -e "${RED}部分 Job 执行失败！${NC}"
    exit 1
else
    echo -e "${GREEN}所有 Job 执行成功！${NC}"
    exit 0
fi
