package com.liceo.campustrack.repository;

import com.liceo.campustrack.model.Facility;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Repository
public class FacilityRepository extends TextFileRepository<Facility> {

    public FacilityRepository(@Value("${campustrack.data-dir}") String dataDir) {
        super(dataDir, "facilities.txt", "id|name|code|location|capacity|description|active");
    }

    @Override protected Long getId(Facility facility) { return facility.getId(); }
    @Override protected void setId(Facility facility, Long id) { facility.setId(id); }

    @Override
    protected String[] toFields(Facility f) {
        return new String[] {
                text(f.getId()), text(f.getName()), text(f.getCode()),
                text(f.getLocation()), text(f.getCapacity()), text(f.getDescription()), text(f.getActive())
        };
    }

    @Override
    protected Facility fromFields(String[] f) {
        Facility facility = new Facility();
        facility.setId(number(f, 0));
        facility.setName(field(f, 1));
        facility.setCode(field(f, 2));
        facility.setLocation(field(f, 3));
        facility.setCapacity(integer(f, 4));
        facility.setDescription(field(f, 5));
        facility.setActive(!"false".equalsIgnoreCase(field(f, 6)));
        return facility;
    }
}
