package org.openmrs.module.kenyaemrIL.metadata;

import org.openmrs.module.metadatadeploy.bundle.AbstractMetadataBundle;
import org.springframework.stereotype.Component;

import static org.openmrs.module.metadatadeploy.bundle.CoreConstructors.*;

/**
 * Implementation of access control to the app.
 */
@Component
public class ILAdminSecurityMetadata extends AbstractMetadataBundle{

    public static class _Privilege {
        public static final String APP_IL_ADMIN = "App: kenyaemrilladmin.home";
        public static final String APP_USHAURI_ADMIN = "App: kenyaemr.ushauri.home";
    }

    public static final class _Role {
        public static final String APPLICATION_IL_ADMIN = "IL Administration";
        public static final String APPLICATION_USHAURI_ADMIN = "Ushauri Administration";
        public static final String API_PRIVILEGES_VIEW_AND_EDIT = "API Privileges (View and Edit)";
    }

    /**
     * @see AbstractMetadataBundle#install()
     */
    @Override
    public void install() {

        install(privilege(_Privilege.APP_IL_ADMIN, "Able to refresh IL Messages"));
        install(privilege(_Privilege.APP_USHAURI_ADMIN, "Able to manage data exchange with Ushauri server"));
        install(role(_Role.APPLICATION_IL_ADMIN, "Can access IL Admin app", idSet(
                _Role.API_PRIVILEGES_VIEW_AND_EDIT
        ), idSet(
                _Privilege.APP_IL_ADMIN
        )));

        install(role(_Role.APPLICATION_USHAURI_ADMIN, "Can access Ushauri Admin app", idSet(
                _Role.API_PRIVILEGES_VIEW_AND_EDIT
        ), idSet(
                _Privilege.APP_USHAURI_ADMIN
        )));
    }
}
