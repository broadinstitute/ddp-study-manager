import {Pipe, PipeTransform} from "@angular/core";
import {DrugList} from "../drug-list/drug-list.model";

@Pipe( {
  name: "drugFilter"
  // pure: false
} )
export class DrugFilterPipe implements PipeTransform {

  transform( array: DrugList[], filterDisplayName: string, filterGenericName: string, filterBrandName: string, filterChemocat: string,
             filterChemoType: string, filterTreatmentType: string, filterChemotherapy: string ): DrugList[] {
    if (filterDisplayName != "") {
      array = array.filter( row => row.displayName != null && row.displayName.indexOf( filterDisplayName.toUpperCase() ) > -1 );
    }
    if (filterGenericName != "") {
      array = array.filter( row => row.genericName != null && row.genericName.indexOf( filterGenericName.toUpperCase() ) > -1 );
    }
    if (filterBrandName != "") {
      array = array.filter( row => row.brandName != null && row.brandName.indexOf( filterBrandName.toUpperCase() ) > -1 );
    }
    if (filterChemocat != "") {
      array = array.filter( row => row.chemocat != null && row.chemocat.indexOf( filterChemocat.toUpperCase() ) > -1 );
    }
    if (filterChemoType != "") {
      array = array.filter( row => row.chemoType != null && row.chemoType.indexOf( filterChemoType ) > -1 );
    }
    if (filterTreatmentType != "") {
      array = array.filter( row => row.treatmentType != null && row.treatmentType.indexOf( filterTreatmentType ) > -1 );
    }
    if (filterChemotherapy != "") {
      array = array.filter( row => row.chemotherapy != null && row.chemotherapy.indexOf( filterChemotherapy ) > -1 );
    }
    return array;
  }

}
