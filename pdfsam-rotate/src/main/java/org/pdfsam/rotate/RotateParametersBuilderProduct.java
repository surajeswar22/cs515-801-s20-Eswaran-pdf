package org.pdfsam.rotate;

import org.pdfsam.task.PdfRotationInput;
import org.sejda.commons.collection.NullSafeSet;
import org.sejda.model.input.PdfSource;
import org.sejda.model.pdf.page.PageRange;
import org.sejda.model.pdf.page.PredefinedSetOfPages;
import org.sejda.model.rotation.Rotation;

import java.util.Set;

import static java.util.Objects.isNull;

public class RotateParametersBuilderProduct {
    private Set<PdfRotationInput> Inputs = new NullSafeSet<>();
    private Rotation rotate;
    private PredefinedSetOfPages predefinedRotationType;

    public Set<PdfRotationInput> getInputs() {
        return Inputs;
    }

    public void setRotate(Rotation rotate) {
        this.rotate = rotate;
    }

    public void setPredefinedRotationType(PredefinedSetOfPages predefinedRotationType) {
        this.predefinedRotationType = predefinedRotationType;
    }

    public boolean hasInput() {
        return !Inputs.isEmpty();
    }

    public void addInput(PdfSource<?> source, Set<PageRange> pageSelection) {
        int s, e; //Setting variable for accessing the starting page and end page
        if (isNull(pageSelection) || pageSelection.isEmpty()) {
            this.Inputs.add(new PdfRotationInput(source, rotate, predefinedRotationType));
        } else {
            //ADDING FEATURES FOR ODD AND EVEN PAGES @Modified Suraj Eswaran
            Set<PageRange> NewPage = new NullSafeSet<>();
            for (PageRange pr : pageSelection) {
                switch (predefinedRotationType) {
                    // Here for Rotation we have three cases 1.Rotation of Odd number of pages 2. Rotation of Even number of pages 3. Rotation of all the pages
                    //ODD PAGES CASE
                    case ODD_PAGES: {
                        s = pr.getStart() % 2 == 0 ? pr.getStart() + 1 : pr.getStart(); // getStart()-> Get the starting offset number that was used for pagination
                        e = pr.getEnd() % 2 == 0 ? pr.getEnd() - 1 : pr.getEnd();//getEnd()->Get the ending offset number that was used for pagination.
                        if (e % 2 != 0 && e < s) //check if modulus of end value with 2 is not equal to zero to prove its odd in number
                        {
                            NewPage.add(new PageRange(e, e)); //Add a new page range of end offset value
                        } else {
                            for (int i = s; i <= e; i += 2)
                                NewPage.add(new PageRange(i, i));
                        }
                        break;
                    }
                    //EVEN PAGES CASE
                    case EVEN_PAGES: {
                        s = pr.getStart() % 2 == 1 ? pr.getStart() + 1 : pr.getStart();// getStart()-> Get the starting offset number that was used for pagination
                        e = pr.getEnd() % 2 == 1 ? pr.getEnd() - 1 : pr.getEnd();//getEnd()->Get the ending offset number that was used for pagination.
                        if (e < s && e % 2 == 0) //check if modulus of end value with 2 is equal to zero to prove its even in number
                        {
                            NewPage.add(new PageRange(e, e));//Add a new page range of end offset value
                        } else {
                            for (int i = s; i <= e; i += 2) //pointer i to the starting value s and goes till ending value e
                                NewPage.add(new PageRange(i, i)); //adding up of new page with the value i
                        }
                        break;
                    }
                    //ALL PAGES CASES
                    case ALL_PAGES: {
                        this.Inputs.add(new PdfRotationInput(source, rotate, pageSelection.stream().toArray(PageRange[]::new))); //Rotating all the pages with the list of pages given
                        return;
                    }
                }
            }
            this.Inputs.add(new PdfRotationInput(source, rotate, NewPage.stream().toArray(PageRange[]::new))); //Rotating all the pages with the list of pages given
        }
    }
}