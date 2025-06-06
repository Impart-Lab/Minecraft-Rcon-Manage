package cc.endmc.server.domain.permission;

import cc.endmc.common.annotation.Excel;
import cc.endmc.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 管理员名单对象 operator_list
 *
 * @author Memory
 * @date 2025-01-11
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class OperatorList extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    private Long id;

    /**
     * 玩家昵称
     */
    @Excel(name = "玩家昵称")
    private String userName;

    /**
     * 状态
     */
    @Excel(name = "状态")
    private Long status;

    /**
     * 其他参数
     */
    @Excel(name = "其他参数")
    private String parameter;

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("userName", getUserName())
                .append("status", getStatus())
                .append("parameter", getParameter())
                .append("createTime", getCreateTime())
                .append("updateTime", getUpdateTime())
                .append("createBy", getCreateBy())
                .append("updateBy", getUpdateBy())
                .append("remark", getRemark())
                .toString();
    }
}
