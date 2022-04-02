package com.gallop.wechat.entity;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;

/**
 * author gallop
 * date 2022-03-25 17:30
 * Description:
 * Modified By:
 */
@Data
@Entity
@Table(schema = "qywx_api", name = "qywx_third_company")
public class QywxThirdCompany implements Serializable {
    private static final long serialVersionUID = 4362326685566509613L;

    @Id
    //自增长主键
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "corp_id")
    private String corpId;
    @Column(name = "permanent_code")
    private String permanentCode;
    @Column(name = "corp_name")
    private String corpName;
    @Column(name = "corp_full_name")
    private String corpFullName;
    @Column(name = "subject_type")
    private Integer subjectType;//企业类型，1. 企业; 2. 政府以及事业单位; 3. 其他组织, 4.团队号
    @Column(name = "agent_id")
    private Integer agentId; //授权方应用id
    @Column(name = "agent_name")
    private String agentName;//授权方应用名字
    private Integer status;
}
