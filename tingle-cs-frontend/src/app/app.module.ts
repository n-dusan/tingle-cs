  import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { FlexLayoutModule } from '@angular/flex-layout';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { MatSidenavModule } from '@angular/material/sidenav';
import { MatButtonModule } from '@angular/material/button';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCardModule } from '@angular/material/card';
import { MatSelectModule } from '@angular/material/select';
import { MatStepperModule } from '@angular/material/stepper';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDividerModule } from '@angular/material/divider';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatRadioModule } from '@angular/material/radio';

import { LoginComponent } from './auth/login/login.component';
import { HeaderComponent } from './navigation/header/header.component';
import { SidenavListComponent } from './navigation/sidenav-list/sidenav-list.component';
import { SignupComponent } from './auth/signup/signup.component';
import { HomePageComponent } from './home-page/home-page.component';
import { AllCertificatesComponent } from './all-certificates/all-certificates.component';
import { AllCertificatesListComponent } from './all-certificates/all-certificates-list/all-certificates-list.component';
import { AllCertificatesItemComponent } from './all-certificates/all-certificates-item/all-certificates-item.component';
import { HttpClientModule } from '@angular/common/http';
import { CertificateIssuingComponent } from './certificate-issuing/certificate-issuing.component';
import { RevokeDialogComponent } from './all-certificates/revoke-dialog/revoke-dialog.component';
import { CheckCertificateComponent } from './check-certificate/check-certificate.component';

import { ToastrModule } from 'ngx-toastr';



@NgModule({
  declarations: [
    AppComponent,
    LoginComponent,
    HeaderComponent,
    SidenavListComponent,
    SignupComponent,
    HomePageComponent,
    AllCertificatesComponent,
    AllCertificatesListComponent,
    AllCertificatesItemComponent,
    CertificateIssuingComponent,
    RevokeDialogComponent,
    CheckCertificateComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    BrowserAnimationsModule,
    FlexLayoutModule,
    FormsModule,
    ReactiveFormsModule,
    MatSidenavModule,
    MatToolbarModule,
    MatIconModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatCardModule,
    HttpClientModule,
    MatSelectModule,
    MatStepperModule,
    MatSlideToggleModule,
    MatCheckboxModule,
    MatDividerModule,
    MatTableModule,
    MatProgressSpinnerModule,
    MatDialogModule,
    MatTooltipModule,
    MatPaginatorModule,
    MatRadioModule,
    ToastrModule.forRoot()
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
