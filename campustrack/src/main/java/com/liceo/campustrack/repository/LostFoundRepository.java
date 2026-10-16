package com.liceo.campustrack.repository;

import com.liceo.campustrack.model.ItemStatus;
import com.liceo.campustrack.model.ItemType;
import com.liceo.campustrack.model.LostFoundItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Repository
public class LostFoundRepository extends TextFileRepository<LostFoundItem> {

    public LostFoundRepository(@Value("${campustrack.data-dir}") String dataDir) {
        super(dataDir, "lost-found.txt",
                "id|title|description|category|itemType|location|itemDate|photoFile|status|reporterId"
                        + "|verificationQuestion|verificationAnswerHash|claimedById|createdAt");
    }

    @Override protected Long getId(LostFoundItem item) { return item.getId(); }
    @Override protected void setId(LostFoundItem item, Long id) { item.setId(id); }

    @Override
    protected String[] toFields(LostFoundItem i) {
        return new String[] {
                text(i.getId()), text(i.getTitle()), text(i.getDescription()), text(i.getCategory()),
                text(i.getItemType()), text(i.getLocation()), text(i.getItemDate()),
                text(i.getPhotoFile()), text(i.getStatus()), text(i.getReporterId()),
                text(i.getVerificationQuestion()), text(i.getVerificationAnswerHash()),
                text(i.getClaimedById()), text(i.getCreatedAt())
        };
    }

    @Override
    protected LostFoundItem fromFields(String[] f) {
        LostFoundItem i = new LostFoundItem();
        i.setId(number(f, 0));
        i.setTitle(field(f, 1));
        i.setDescription(field(f, 2));
        i.setCategory(field(f, 3));
        i.setItemType(constant(ItemType.class, f, 4));
        i.setLocation(field(f, 5));
        i.setItemDate(date(f, 6));
        i.setPhotoFile(field(f, 7));
        i.setStatus(constant(ItemStatus.class, f, 8));
        i.setReporterId(number(f, 9));
        i.setVerificationQuestion(field(f, 10));
        i.setVerificationAnswerHash(field(f, 11));
        i.setClaimedById(number(f, 12));
        i.setCreatedAt(dateTime(f, 13));
        return i;
    }
}
