import {HomeComponent} from "./home/home.component";
import {NDIUploadComponent} from "./ndiupload/ndiupload.component";
import {ParticipantListComponent} from "./participant-list/participant-list.component";
import {ScanComponent} from "./scan/scan.component";
import {AuthGuard} from "./auth0/auth.guard";
import {PermalinkComponent} from "./permalink/permalink.component";
import {DashboardComponent} from "./dashboard/dashboard.component";
import {MailingListComponent} from "./mailing-list/mailing-list.component";
import {ShippingComponent} from "./shipping/shipping.component";
import {UploadComponent} from "./upload/upload.component";
import {ParticipantExitComponent} from "./participant-exit/participant-exit.component";
import {UserSettingComponent} from "./user-setting/user-setting.component";
import {Statics} from "./utils/statics";
import {SurveyComponent} from "./survey/survey.component";
import {ShippingReportComponent} from "./shipping-report/shipping-report.component";
import {LabelSettingsComponent} from "./label-settings/label-settings.component";
import {DrugListComponent} from "./drug-list/drug-list.component";
import {ParticipantEventComponent} from "./participant-event/participant-event.component";
import {ShippingSearchComponent} from "./shipping-search/shipping-search.component";
import {DiscardSampleComponent} from "./discard-sample/discard-sample.component";
import {DiscardSamplePageComponent} from "./discard-sample-page/discard-sample-page.component";
import {PdfDownloadComponent} from "./pdf-download/pdf-download.component";
import {TissueListComponent} from "./tissue-list/tissue-list.component";

import {AbstractionSettingsComponent} from "./abstraction-settings/abstraction-settings.component";
import {DataReleaseComponent} from "./data-release/data-release.component";
import {TissuePageComponent} from "./tissue-page/tissue-page.component";
import {MedicalRecordComponent} from "./medical-record/medical-record.component";
import {ParticipantPageComponent} from "./participant-page/participant-page.component";
import {FieldSettingsComponent} from "./field-settings/field-settings.component";

export const AppRoutes = [

  {path: "", redirectTo: Statics.HOME, pathMatch: "full"},
  {path: Statics.HOME, component: HomeComponent},

  //Samples
  {path: Statics.UNSENT_OVERVIEW, component: DashboardComponent, canActivate: [ AuthGuard ]},
  {path: Statics.SHIPPING_DASHBOARD, component: DashboardComponent, canActivate: [ AuthGuard ]},
  {path: "shippingReport", component: ShippingReportComponent, canActivate: [ AuthGuard ]},

  {path: Statics.SHIPPING_QUEUE, component: ShippingComponent, canActivate: [ AuthGuard ]},
  {path: Statics.SHIPPING_ERROR, component: ShippingComponent, canActivate: [ AuthGuard ]},
  {path: Statics.SHIPPING_SENT, component: ShippingComponent, canActivate: [ AuthGuard ]},
  {path: Statics.SHIPPING_RECEIVED, component: ShippingComponent, canActivate: [ AuthGuard ]},
  {path: Statics.SHIPPING_OVERVIEW, component: ShippingComponent, canActivate: [ AuthGuard ]},
  {path: Statics.SHIPPING_DEACTIVATED, component: ShippingComponent, canActivate: [ AuthGuard ]},
  {path: Statics.SHIPPING_UPLOADED, component: ShippingComponent, canActivate: [ AuthGuard ]},
  {path: Statics.SHIPPING_TRIGGERED, component: ShippingComponent, canActivate: [ AuthGuard ]},

  {path: "scan", component: ScanComponent, canActivate: [ AuthGuard ]},
  {path: "shippingSearch", component: ShippingSearchComponent, canActivate: [ AuthGuard ]},
  {path: "upload", component: UploadComponent, canActivate: [ AuthGuard ]},
  {path: "discardList", component: DiscardSampleComponent, canActivate: [ AuthGuard ]},
  {path: "discardSample", component: DiscardSamplePageComponent, canActivate: [ AuthGuard ]},
  {path: "labelSettings", component: LabelSettingsComponent, canActivate: [ AuthGuard ]},
  {path: "drugList", component: DrugListComponent, canActivate: [ AuthGuard ]},

  //Study
  {path: Statics.MEDICALRECORD_DASHBOARD, component: DashboardComponent, canActivate: [ AuthGuard ]},
  {path: "participantList", component: ParticipantListComponent, canActivate: [ AuthGuard ]},
  {path: "tissueList", component: TissueListComponent, canActivate: [ AuthGuard ]},

//  { path: Statics.PARTICIPANT, component: ParticipantComponent, canActivate: [AuthGuard] },
  {path: "participantPage", component: ParticipantPageComponent, canActivate: [ AuthGuard ]},
  {path: Statics.MEDICALRECORD, component: MedicalRecordComponent, canActivate: [ AuthGuard ]},
  {path: "tissue", component: TissuePageComponent, canActivate: [ AuthGuard ]},

  {path: "fieldSettings", component: FieldSettingsComponent, canActivate: [ AuthGuard ]},
  {path: "dataRelease", component: DataReleaseComponent, canActivate: [ AuthGuard ]},
  {path: "medicalRecordAbstractionSettings", component: AbstractionSettingsComponent, canActivate: [ AuthGuard ]},
  // { path: 'dataRelease', component: DataReleaseComponent, canActivate: [AuthGuard] },

  {path: "mailingList", component: MailingListComponent, canActivate: [ AuthGuard ]},
  {path: "participantExit", component: ParticipantExitComponent, canActivate: [ AuthGuard ]},
  {path: "survey", component: SurveyComponent, canActivate: [ AuthGuard ]},
  {path: "participantEvent", component: ParticipantEventComponent, canActivate: [ AuthGuard ]},
  {path: "downloadPDF", component: PdfDownloadComponent, canActivate: [ AuthGuard ]},
  {path: "customizeView", component: ShippingSearchComponent, canActivate: [ AuthGuard ]},
  {path: "ndi", component: NDIUploadComponent, canActivate: [ AuthGuard ]},

  {path: "userSettings", component: UserSettingComponent, canActivate: [ AuthGuard ]},

  //Permalink
  {path: Statics.PERMALINK + Statics.MEDICALRECORD_URL + "/:participantid/:medicalrecordid", component: PermalinkComponent, canActivate: [ AuthGuard ]},
  {path: Statics.PERMALINK + Statics.SHIPPING_URL, component: ShippingComponent, canActivate: [ AuthGuard ]},
  {path: Statics.PERMALINK + Statics.UNSENT_OVERVIEW_URL, component: DashboardComponent, canActivate: [ AuthGuard ]},
  {path: "**", redirectTo: "/home"}

];
