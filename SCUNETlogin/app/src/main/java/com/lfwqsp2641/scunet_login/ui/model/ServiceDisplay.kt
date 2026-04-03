package com.lfwqsp2641.scunet_login.ui.model

import com.lfwqsp2641.scunet_login.R
import com.lfwqsp2641.scunet_login.data.enums.ServiceType

enum class ServiceDisplay(
    val serviceType: ServiceType, val label: Int
) {
    EduNet(ServiceType.EduNet, R.string.edunet), ChinaTelecom(
        ServiceType.ChinaTelecom,
        R.string.china_telecom
    ),
    ChinaMobile(
        ServiceType.ChinaMobile,
        R.string.china_mobile
    ),
    ChinaUnicom(ServiceType.ChinaUnicom, R.string.china_unicom)
}
