/* ==================================================================
 * MetserviceDayDatumDataSourceTest.java - Oct 18, 2011 4:04:48 PM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
 * 
 * This program is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation; either version 2 of 
 * the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License 
 * along with this program; if not, write to the Free Software 
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 
 * 02111-1307 USA
 * ==================================================================
 */

package net.solarnetwork.node.weather.nz.metservice.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import net.solarnetwork.node.domain.GeneralDayDatum;
import net.solarnetwork.node.test.AbstractNodeTransactionalTest;
import net.solarnetwork.node.weather.nz.metservice.MetserviceDayDatumDataSource;
import org.junit.Test;
import org.springframework.util.ResourceUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Test case for the {@link MetserviceDayDatumDataSource} class.
 * 
 * @author matt
 * @version 1.2
 */
public class MetserviceDayDatumDataSourceTest extends AbstractNodeTransactionalTest {

	private static final String RISE_SET_RESOURCE_NAME = "riseSet_wellington-city.json";

	private MetserviceDayDatumDataSource createDataSourceInstance() throws Exception {

		URL url = getClass().getResource(RISE_SET_RESOURCE_NAME);
		File f = ResourceUtils.getFile(url);
		String baseDirectory = f.getParent();

		MetserviceDayDatumDataSource ds = new MetserviceDayDatumDataSource();
		ds.setBaseUrl("file://" + baseDirectory);
		ds.setRiseSet(RISE_SET_RESOURCE_NAME);
		ds.setObjectMapper(new ObjectMapper());
		return ds;
	}

	@Test
	public void parseRiseSet() throws Exception {
		MetserviceDayDatumDataSource ds = createDataSourceInstance();
		GeneralDayDatum datum = (GeneralDayDatum) ds.readCurrentDatum();
		assertNotNull(datum);

		final SimpleDateFormat dayFormat = new SimpleDateFormat(ds.getDayDateFormat());

		assertNotNull(datum.getCreated());
		assertEquals("1 September 2014", dayFormat.format(datum.getCreated()));

		final SimpleDateFormat timeFormat = new SimpleDateFormat(ds.getTimeDateFormat());

		assertNotNull(datum.getSunrise());
		assertEquals("6:47am", timeFormat.format(datum.getSunrise().toDateTimeToday().toDate())
				.toLowerCase());

		assertNotNull(datum.getSunset());
		assertEquals("5:56pm", timeFormat.format(datum.getSunset().toDateTimeToday().toDate())
				.toLowerCase());
	}

}
