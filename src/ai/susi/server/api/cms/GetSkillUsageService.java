/**
 *  GetSkillUsageService
 *  Copyright by Anup Kumar Panwar, @anupkumarpanwar
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program in the file lgpl21.txt
 *  If not, see <http://www.gnu.org/licenses/>.
 */

package ai.susi.server.api.cms;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.json.JsonTray;
import ai.susi.mind.SusiSkill;
import ai.susi.server.*;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.File;


/**
 * This Endpoint accepts 4 parameters. model,group,language,skill
 * before getting a rating of a skill, the skill must exist in the directory.
 * http://localhost:4000/cms/getSkillUsage.json?model=general&group=Knowledge&skill=aboutsusi&language=en&duration=7
 */
public class GetSkillUsageService extends AbstractAPIHandler implements APIHandler {


    private static final long serialVersionUID = 1420414106164188352L;

    @Override
    public UserRole getMinimalUserRole() {
        return UserRole.ANONYMOUS;
    }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    @Override
    public String getAPIPath() {
        return "/cms/getSkillUsage.json";
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) {

        String model_name = call.get("model", "general");
        File model = new File(DAO.model_watch_dir, model_name);
        String group_name = call.get("group", "Knowledge");
        File group = new File(model, group_name);
        String language_name = call.get("language", "en");
        File language = new File(group, language_name);
        String skill_name = call.get("skill", null);
        File skill = SusiSkill.getSkillFileInLanguage(language, skill_name, false);
        int duration = Integer.parseInt(call.get("duration", "7"));

        JSONObject result = new JSONObject();
        result.put("accepted", false);
        if (!skill.exists()) {
            result.put("message", "Skill does not exist");
            return new ServiceResponse(result);
        }
        JsonTray skillRating = DAO.skillUsage;
        if (skillRating.has(model_name)) {
            JSONObject modelName = skillRating.getJSONObject(model_name);
            if (modelName.has(group_name)) {
                JSONObject groupName = modelName.getJSONObject(group_name);
                if (groupName.has(language_name)) {
                    JSONObject  languageName = groupName.getJSONObject(language_name);
                    if (languageName.has(skill_name)) {
                        JSONArray skillUsage = languageName.getJSONArray(skill_name);
                        result.put("skill_name", skill_name);
                        JSONArray requiredSkillUsage = new JSONArray();
                        int startIndex = skillUsage.length() >= duration ? skillUsage.length()-duration : 0;
                        int j = 0;
                        for (int i = startIndex; i < skillUsage.length(); i++)
                        {
                            requiredSkillUsage.put(j++, skillUsage.getJSONObject(i));
                        }
                        result.put("skill_usage", requiredSkillUsage);
                        result.put("accepted", true);
                        result.put("message", "Skill usage fetched");
                        return new ServiceResponse(result);
                    }
                }
            }
        }
        result.put("skill_name", skill_name);
        result.put("accepted", false);
        result.put("message", "Skill has not been used yet");
        return new ServiceResponse(result);
    }
}
