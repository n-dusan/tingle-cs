import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { LoginComponent } from './auth/login/login.component';
import { SignupComponent } from './auth/signup/signup.component';
import { MakeRequestComponent } from './make-request/make-request.component';
import { HomePageComponent } from './home-page/home-page.component';
import { AllCertificatesComponent } from './all-certificates/all-certificates.component';


const routes: Routes = [
  {path:'', redirectTo: '/home-page', pathMatch: 'full'},
  {path: 'login', component: LoginComponent},
  {path: 'signup', component: SignupComponent},
  {path: 'make-request', component: MakeRequestComponent},
  {path: 'home-page', component: HomePageComponent},
  {path: 'all-certificates', component: AllCertificatesComponent}
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
