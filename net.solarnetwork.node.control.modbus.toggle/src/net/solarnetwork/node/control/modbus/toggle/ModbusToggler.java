/* ==================================================================
 * ModbusToggler.java - Jul 15, 2013 7:48:20 AM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.control.modbus.toggle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import net.solarnetwork.domain.NodeControlInfo;
import net.solarnetwork.domain.NodeControlPropertyType;
import net.solarnetwork.node.NodeControlProvider;
import net.solarnetwork.node.domain.NodeControlInfoDatum;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnectionAction;
import net.solarnetwork.node.io.modbus.ModbusDeviceSupport;
import net.solarnetwork.node.io.modbus.ModbusSerialConnectionFactory;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus.InstructionState;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import org.springframework.context.MessageSource;

/**
 * Control a Modbus "coil" type register to turn a switch on or off.
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>address</dt>
 * <dd>The Modbus address of the coil-type register to use.</dd>
 * <dt>unitId</dt>
 * <dd>The Modbus unit ID to use.</dd>
 * <dt>controlId</dt>
 * <dd>The {@link NodeControlProvider} UID to use.</dd>
 * <dt>connectionFactory</dt>
 * <dd>The {@link ModbusSerialConnectionFactory} to use.</dd>
 * </dl>
 * 
 * @author matt
 * @version 1.0
 */
public class ModbusToggler extends ModbusDeviceSupport implements SettingSpecifierProvider,
		NodeControlProvider, InstructionHandler {

	private Integer address = 0x4008;

	private String controlId = "/switch/1";
	private MessageSource messageSource;

	@Override
	protected Map<String, Object> readDeviceInfo(ModbusConnection conn) {
		return null;
	}

	/**
	 * Get the values of the discreet values, as a Boolean.
	 * 
	 * @return Boolean for the switch status
	 */
	private synchronized Boolean currentValue() throws IOException {
		BitSet result = performAction(new ModbusConnectionAction<BitSet>() {

			@Override
			public BitSet doWithConnection(ModbusConnection conn) throws IOException {
				return conn.readDiscreetValues(new Integer[] { address }, 1);
			}
		});
		if ( log.isInfoEnabled() ) {
			log.info("Read {} value: {}", controlId, result.get(0));
		}
		return result.get(0);
	}

	private synchronized Boolean setValue(Boolean desiredValue) throws IOException {
		final BitSet bits = new BitSet(1);
		bits.set(0, desiredValue);
		log.info("Setting {} value to {}", controlId, desiredValue);
		final Integer[] addresses = new Integer[] { address };
		return performAction(new ModbusConnectionAction<Boolean>() {

			@Override
			public Boolean doWithConnection(ModbusConnection conn) throws IOException {
				return conn.writeDiscreetValues(addresses, bits);
			}
		});
	}

	// NodeControlProvider

	@Override
	public List<String> getAvailableControlIds() {
		return Collections.singletonList(controlId);
	}

	@Override
	public String getUID() {
		return getControlId();
	}

	@Override
	public NodeControlInfo getCurrentControlInfo(String controlId) {
		// read the control's current status
		log.debug("Reading {} status", controlId);
		NodeControlInfoDatum result = null;
		try {
			Boolean value = currentValue();
			result = newNodeControlInfoDatum(controlId, value);
		} catch ( Exception e ) {
			log.error("Error reading {} status: {}", controlId, e.getMessage());
		}
		return result;
	}

	private NodeControlInfoDatum newNodeControlInfoDatum(String controlId, Boolean status) {
		NodeControlInfoDatum info = new NodeControlInfoDatum();
		info.setCreated(new Date());
		info.setSourceId(controlId);
		info.setType(NodeControlPropertyType.Boolean);
		info.setReadonly(false);
		info.setValue(status.toString());
		return info;
	}

	// InstructionHandler

	@Override
	public boolean handlesTopic(String topic) {
		return InstructionHandler.TOPIC_SET_CONTROL_PARAMETER.equals(topic);
	}

	@Override
	public InstructionState processInstruction(Instruction instruction) {
		// look for a parameter name that matches a control ID
		InstructionState result = null;
		log.debug("Inspecting instruction {} against control {}", instruction.getId(), controlId);
		for ( String paramName : instruction.getParameterNames() ) {
			log.trace("Got instruction parameter {}", paramName);
			if ( controlId.equals(paramName) ) {
				// treat parameter value as a boolean String
				String str = instruction.getParameterValue(controlId);
				Boolean desiredValue = Boolean.parseBoolean(str);
				Boolean modbusResult = null;
				try {
					modbusResult = setValue(desiredValue);
				} catch ( Exception e ) {
					log.warn("Error handling instruction {} on control {}: {}", instruction.getTopic(),
							controlId, e.getMessage());
				}
				if ( modbusResult != null && modbusResult.booleanValue() ) {
					result = InstructionState.Completed;
				} else {
					result = InstructionState.Declined;
				}
			}
		}
		return result;
	}

	// SettingSpecifierProvider

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.control.modbus.toggle";
	}

	@Override
	public String getDisplayName() {
		return "Modbus Switch Toggler";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		ModbusToggler defaults = new ModbusToggler();
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(20);

		// get current value
		BasicTitleSettingSpecifier status = new BasicTitleSettingSpecifier("status", "N/A", true);
		try {
			Boolean val = currentValue();
			status.setDefaultValue(val.toString());
		} catch ( Exception e ) {
			log.debug("Error reading {} status: {}", controlId, e.getMessage());
		}
		results.add(status);

		results.add(new BasicTextFieldSettingSpecifier("controlId", defaults.controlId));
		results.add(new BasicTextFieldSettingSpecifier("groupUID", defaults.getGroupUID()));
		results.add(new BasicTextFieldSettingSpecifier("modbusNetwork.propertyFilters['UID']",
				"Serial Port"));
		results.add(new BasicTextFieldSettingSpecifier("unitId", String.valueOf(defaults.getUnitId())));
		results.add(new BasicTextFieldSettingSpecifier("address", defaults.address.toString()));

		return results;
	}

	@Override
	public MessageSource getMessageSource() {
		return messageSource;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public Integer getAddress() {
		return address;
	}

	public void setAddress(Integer address) {
		this.address = address;
	}

	public String getControlId() {
		return controlId;
	}

	public void setControlId(String controlId) {
		this.controlId = controlId;
	}

}
