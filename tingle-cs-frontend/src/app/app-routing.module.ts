import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { LoginComponent } from './auth/login/login.component';
import { SignupComponent } from './auth/signup/signup.component';
import { HomePageComponent } from './home-page/home-page.component';
import { AllCertificatesComponent } from './all-certificates/all-certificates.component';
import { CertificateIssuingComponent } from './certificate-issuing/certificate-issuing.component';
import { CheckCertificateComponent } from './check-certificate/check-certificate.component';


const routes: Routes = [
  {path:'', redirectTo: '/home-page', pathMatch: 'full'},
  {path: 'login', component: LoginComponent},
  {path: 'signup', component: SignupComponent},
  {path: 'home-page', component: HomePageComponent},
  {path: 'all-certificates', component: AllCertificatesComponent},
  {path: 'issue', component: CertificateIssuingComponent}
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
