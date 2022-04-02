package com.gallop.wechat.repository;

import com.gallop.wechat.entity.QywxThirdCompany;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.Repository;

/**
 * author gallop
 * date 2022-03-26 18:48
 * Description:
 * Modified By:
 */

public interface CompanyRepository extends JpaRepository<QywxThirdCompany,Integer> {
    QywxThirdCompany findByCorpId(String corpId);

}
