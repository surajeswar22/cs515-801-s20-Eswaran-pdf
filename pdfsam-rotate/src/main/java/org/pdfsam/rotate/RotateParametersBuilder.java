/*
 * This file is part of the PDF Split And Merge source code
 * Created on 26/giu/2014
 * Copyright 2017 by Sober Lemur S.a.s. di Vacondio Andrea (info@pdfsam.org).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.pdfsam.rotate;

import static java.util.Objects.isNull;
import java.util.Set;
import org.pdfsam.support.params.AbstractPdfOutputParametersBuilder;
import org.pdfsam.support.params.MultipleOutputTaskParametersBuilder;
import org.pdfsam.task.BulkRotateParameters;
import org.pdfsam.task.PdfRotationInput;
import org.sejda.commons.collection.NullSafeSet;
import org.sejda.model.input.PdfSource;
import org.sejda.model.output.SingleOrMultipleTaskOutput;
import org.sejda.model.pdf.page.PageRange;
import org.sejda.model.pdf.page.PredefinedSetOfPages;
import org.sejda.model.rotation.Rotation;

/**
 * Builder for {@link BulkRotateParameters}
 *
 * @author Andrea Vacondio
 *
 * @Modified Suraj Eswaran
 */
class RotateParametersBuilder extends AbstractPdfOutputParametersBuilder<BulkRotateParameters>
        implements MultipleOutputTaskParametersBuilder<BulkRotateParameters> {

    private SingleOrMultipleTaskOutput output;
    private String prefix;
    private Set<PdfRotationInput> Inputs = new NullSafeSet<>();
    private Rotation rotate;
    private PredefinedSetOfPages predefinedRotationType;

    void addInput(PdfSource<?> source, Set<PageRange> pageSelection) {
        int s,e; //Setting variable for accessing the starting page and end page
        if (isNull(pageSelection) || pageSelection.isEmpty()) {
            this.Inputs.add(new PdfRotationInput(source, rotate, predefinedRotationType));
        }
        else {
            //ADDING FEATURES FOR ODD AND EVEN PAGES @Modified Suraj Eswaran
            Set<PageRange> NewPage=new NullSafeSet<>();
            for(PageRange pr : pageSelection) {
                switch (predefinedRotationType) {
                    // Here for Rotation we have three cases 1.Rotation of Odd number of pages 2. Rotation of Even number of pages 3. Rotation of all the pages
                                        //EVEN PAGES CASE
                    case EVEN_PAGES: {
                         s = pr.getStart()%2 == 1 ? pr.getStart()+1 : pr.getStart();// getStart()-> Get the starting offset number that was used for pagination
                         e = pr.getEnd()%2 == 1 ? pr.getEnd()-1 : pr.getEnd();//getEnd()->Get the ending offset number that was used for pagination.
                        if (e < s && e %2 == 0) //check if modulus of end value with 2 is equal to zero to prove its even in number
                        {
                            NewPage.add(new PageRange(e,e));//Add a new page range of end offset value
                        }
                        else {
                            for (int i = s; i <= e; i += 2) //pointer i to the starting value s and goes till ending value e
                                NewPage.add(new PageRange(i, i)); //adding up of new page with the value i
                        }
                        break;
                    }
                    //ODD PAGES CASE
                    case ODD_PAGES: {
                         s = pr.getStart()%2 == 0 ? pr.getStart()+1 : pr.getStart(); // getStart()-> Get the starting offset number that was used for pagination
                         e = pr.getEnd()%2 == 0 ? pr.getEnd()-1 : pr.getEnd();//getEnd()->Get the ending offset number that was used for pagination.
                        if (e %2 != 0 && e < s) //check if modulus of end value with 2 is not equal to zero to prove its odd in number
                        {
                            NewPage.add(new PageRange(e,e)); //Add a new page range of end offset value
                        }
                        else {
                            for (int j = s; j <= e; j += 2)
                                NewPage.add(new PageRange(j, j));
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

    boolean hasInput() {
        return !Inputs.isEmpty();
    }

    @Override
    public void output(SingleOrMultipleTaskOutput output) {
        this.output = output;
    }

    @Override
    public void prefix(String prefix) {
        this.prefix = prefix;
    }

    protected SingleOrMultipleTaskOutput getOutput() {
        return output;
    }

    protected String getPrefix() {
        return prefix;
    }

    public void rotation(Rotation rotation) {
        this.rotate = rotation;
    }

    public void rotationType(PredefinedSetOfPages predefinedRotationType) {
        this.predefinedRotationType = predefinedRotationType;

    }

    @Override
    public BulkRotateParameters build() {
        BulkRotateParameters params = new BulkRotateParameters();
        params.setCompress(isCompress());
        params.setExistingOutputPolicy(existingOutput());
        params.setVersion(getVersion());
        params.setOutput(getOutput());
        params.setOutputPrefix(getPrefix());
        Inputs.forEach(params::addInput);
        return params;
    }

}
