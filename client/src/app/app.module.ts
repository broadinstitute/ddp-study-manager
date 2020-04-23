import {BrowserModule} from "@angular/platform-browser";
import {NgModule} from "@angular/core";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {HttpModule} from "@angular/http";
import {RouterModule} from "@angular/router";
import {DataTableModule} from "angular2-datatable";
import {
  AccordionModule,
  CollapseModule,
  DatepickerModule,
  ModalModule,
  SortableModule,
  TabsModule,
  TooltipModule,
  TypeaheadModule
} from "ngx-bootstrap";
import {
  MdAutocompleteModule,
  MdButtonModule,
  MdButtonToggleModule,
  MdCheckboxModule,
  MdInputModule,
  MdRadioModule,
  MdSelectModule
} from "@angular/material";
import {BrowserAnimationsModule} from "@angular/platform-browser/animations";
import {CookieModule} from "ngx-cookie";
import {NgxPaginationModule} from "ngx-pagination";

import {AppComponent} from "./app.component";
import {FieldMultiTypeArrayComponent} from "./field-multi-type-array/field-multi-type-array.component";
import {FieldMultiTypeComponent} from "./field-multi-type/field-multi-type.component";
import {HomeComponent} from "./home/home.component";
import {AppRoutes} from "./app.routing";
import {KitLabelComponent} from "./kit-label/kit-label.component";
import {AddressComponent} from "./address/address.component";
import {NDIUploadComponent} from "./ndiupload/ndiupload.component";
import {DrugFilterPipe} from "./pipe/drug-filter.pipe";
import {OrdinalPipe} from "./pipe/ordinal.pipe";
import {DSMService} from "./services/dsm.service";
import {ScanComponent} from "./scan/scan.component";
import {AuthGuard} from "./auth0/auth.guard";
import {Auth} from "./services/auth.service";
import {ScanPairComponent} from "./scan-pair/scan-pair.component";
import {SessionService} from "./services/session.service";
import {RoleService} from "./services/role.service";
import {ModalComponent} from "./modal/modal.component";
import {MedicalRecordComponent} from "./medical-record/medical-record.component";
import {AssigneesComponent} from "./assignee/assignee.component";
import {ParticipantPageComponent} from "./participant-page/participant-page.component";
import {ComponentService} from "./services/component.service";
import {PermalinkComponent} from "./permalink/permalink.component";
import {MedicalRecordLogSortPipe} from "./pipe/medical-record-log-sort.pipe";
import {Utils} from "./utils/utils";
import {DashboardComponent} from "./dashboard/dashboard.component";
import {DashboardDateSortPipe} from "./pipe/dashboard-date-sort.pipe";
import {KitRequestSortPipe} from "./pipe/kit-request-sort.pipe";
import {MailingListComponent} from "./mailing-list/mailing-list.component";
import {ErrorLabelComponent} from "./error-label/error-label.component";
import {ShippingComponent} from "./shipping/shipping.component";
import {UploadComponent} from "./upload/upload.component";
import {ParticipantExitComponent} from "./participant-exit/participant-exit.component";
import {ParticipantExitSortPipe} from "./pipe/participant-exit-sort.pipe";
import {UserSettingComponent} from "./user-setting/user-setting.component";
import {Statics} from "./utils/statics";
import {ScanValueComponent} from "./scan-value/scan-value.component";
import {BannerComponent} from "./banner/banner.component";
import {FieldDatepickerComponent} from "./field-datepicker/field-datepicker.component";
import {OncHistoryDetailComponent} from "./onc-history-detail/onc-history-detail.component";
import {TissuePageComponent} from "./tissue-page/tissue-page.component";
import {TissueComponent} from "./tissue/tissue.component";
import {LookupComponent} from "./lookup/lookup.component";
import {SurveyComponent} from "./survey/survey.component";
import {KitRequestFilterPipe} from "./pipe/kit-request-filter.pipe";
import {ShippingReportComponent} from "./shipping-report/shipping-report.component";
import {OncHistoryDetailSortPipe} from "./pipe/onc-history-detail-sort.pipe";
import {LabelSettingsComponent} from "./label-settings/label-settings.component";
import {DrugListComponent} from "./drug-list/drug-list.component";
import {ParticipantEventComponent} from "./participant-event/participant-event.component";
import {FieldFilepickerComponent} from "./field-filepicker/field-filepicker.component";
import {ShippingSearchComponent} from "./shipping-search/shipping-search.component";
import {SurveySortPipe} from "./pipe/survey-sort.pipe";
import {SurveyFilterPipe} from "./pipe/survey-filter.pipe";
import {DiscardSampleComponent} from "./discard-sample/discard-sample.component";
import {DiscardSamplePageComponent} from "./discard-sample-page/discard-sample-page.component";
import {PdfDownloadComponent} from "./pdf-download/pdf-download.component";
import {DateFormatPipe} from "./pipe/custom-date.pipe";
import {MedicalRecordAbstractionComponent} from "./medical-record-abstraction/medical-record-abstraction.component";
import {AbstractionGroupComponent} from "./abstraction-group/abstraction-group.component";
import {ButtonSelectTitleCasePipe} from "./pipe/button-select-title-case.pipe";
import {NgxPageScrollModule} from "ngx-page-scroll";
import {AbstractionSettingsComponent} from "./abstraction-settings/abstraction-settings.component";
import {DragulaModule} from "ng2-dragula";
import {AbstractionFieldComponent} from "./abstraction-field/abstraction-field.component";
import {DataReleaseComponent} from "./data-release/data-release.component";
import {FieldTypeaheadComponent} from "./field-typeahead/field-typeahead.component";
import {FieldQuestionComponent} from "./field-question/field-question.component";
import {FieldQuestionArrayComponent} from "./field-question-array/field-question-array.component";
import {TissueListComponent} from "./tissue-list/tissue-list.component";
import {FilterColumnComponent} from "./filter-column/filter-column.component";
import {ParticipantListComponent} from "./participant-list/participant-list.component";
import {ActivityDataComponent} from "./activity-data/activity-data.component";
import {SearchBarComponent} from "./search-bar/search-bar.component";
import {FieldSettingsComponent} from "./field-settings/field-settings.component";

@NgModule( {
  declarations: [
    AppComponent,
    HomeComponent,
    KitLabelComponent,
    AddressComponent,
    ScanComponent,
    ScanPairComponent,
    MedicalRecordComponent,
    ModalComponent,
    AssigneesComponent,
    ParticipantPageComponent,
    PermalinkComponent,
    DashboardComponent,
    MailingListComponent,
    ErrorLabelComponent,
    ShippingComponent,
    UploadComponent,
    ParticipantExitComponent,
    UserSettingComponent,
    ScanValueComponent,
    BannerComponent,
    FieldDatepickerComponent,
    OncHistoryDetailComponent,
    TissuePageComponent,
    TissueComponent,
    LookupComponent,
    SurveyComponent,
    ShippingReportComponent,
    DiscardSampleComponent,
    DiscardSamplePageComponent,
    FieldSettingsComponent,


    MedicalRecordLogSortPipe,
    DashboardDateSortPipe,
    KitRequestSortPipe,
    KitRequestFilterPipe,
    ParticipantExitSortPipe,
    OncHistoryDetailSortPipe,
    ButtonSelectTitleCasePipe,
    OrdinalPipe,
    LabelSettingsComponent,
    DrugListComponent,
    ParticipantEventComponent,
    ShippingSearchComponent,
    FieldFilepickerComponent,
    SurveySortPipe,
    SurveyFilterPipe,
    DrugFilterPipe,
    PdfDownloadComponent,
    DateFormatPipe,
    NDIUploadComponent,
    MedicalRecordAbstractionComponent,
    AbstractionGroupComponent,
    AbstractionSettingsComponent,
    AbstractionFieldComponent,
    DataReleaseComponent,
    FieldTypeaheadComponent,
    FieldQuestionComponent,
    FieldQuestionArrayComponent,
    FieldMultiTypeComponent,
    FieldMultiTypeArrayComponent,
    TissueListComponent,
    FilterColumnComponent,
    ParticipantListComponent,
    ActivityDataComponent,
    SearchBarComponent

  ],
  imports: [
    BrowserModule,
    FormsModule,
    RouterModule.forRoot( AppRoutes, {enableTracing: true} ),
    HttpModule,
    ReactiveFormsModule,

    MdCheckboxModule,
    MdButtonModule,
    MdInputModule,
    BrowserAnimationsModule,
    MdSelectModule,
    MdRadioModule,
    MdButtonToggleModule,
    NgxPageScrollModule,
    MdAutocompleteModule,

    DataTableModule,
    NgxPaginationModule,
    TabsModule.forRoot(),
    DatepickerModule.forRoot(),
    TooltipModule.forRoot(),
    CookieModule.forRoot(),
    ModalModule.forRoot(),
    CollapseModule.forRoot(),
    SortableModule.forRoot(),
    CookieModule.forRoot(),
    AccordionModule.forRoot(),
    TypeaheadModule.forRoot(),
    DragulaModule
  ],
  providers: [
    Auth,
    AuthGuard,
    ComponentService,
    DSMService,
    RoleService,
    SessionService,
    Utils,
    Statics
  ],
  bootstrap: [ AppComponent ]
} )
export class AppModule {
}
